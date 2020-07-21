package com.github.beans.factory.refreshaware;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Convinient based factory bean.
 *
 * @param <T> type of object that this factory bean creates
 *            wrapped in {@link org.springframework.aop.framework.AopProxy}
 */
@SuppressWarnings("all")
public abstract class ProxyBasedRefreshAwareFactoryBean<T> extends RefreshAwareFactoryBean<T> {

    /**
     * Proxy with reference to the most recent instance.
     * <p>
     * All proxy method calls will be delegated to underlying
     * instance obtain through {@link #getInstance()}.
     */
    private T proxy;

    /**
     * Type of object that
     * this FactoryBean creates.
     */
    private Class<T> type;

    public ProxyBasedRefreshAwareFactoryBean() {
    }

    public ProxyBasedRefreshAwareFactoryBean(TaskScheduler scheduler) {
        super(scheduler);
    }

    public ProxyBasedRefreshAwareFactoryBean(TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        super(scheduler, beforeRefresh, beforeDestroy);
    }

    /**
     * Expose bean as a Proxy.
     *
     * @return proxy with reference to the most recent instance.
     * @throws Exception in case of any error.
     */
    @NonNull
    public final T getObject() throws Exception {
        return proxy;
    }

    @Override
    public final boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        this.proxy = (T) ProxyFactory.getProxy(new TargetSource() {
            public boolean isStatic() {
                return false;
            }

            public Class<?> getTargetClass() {
                return getObjectType();
            }

            public Object getTarget() {
                return getInstance();
            }

            public void releaseTarget(Object target) throws Exception {
            }
        });
    }

    /**
     * Attempts to auto discover object type managed
     * by this factory bean.
     *
     * @return type of object that this factory creates.
     * @throws IllegalStateException if auto discovery fails
     */
    @NonNull
    @Override
    public Class<T> getObjectType() {
        if (type != null) {
            return type;
        }

        Method[] methods = getClass().getDeclaredMethods();

        Class<?> returnType = null;
        for (Method method : methods) {
            if ("createInstance".equals(method.getName())) {
                Class<?> type = method.getReturnType();

                if (returnType == null) {
                    returnType = type;
                    continue;
                }

                if (type != returnType && returnType.isAssignableFrom(type)) {
                    // found more specific return type
                    returnType = type;
                }
            }
        }

        if (returnType == null) {
            throw new IllegalStateException("createInstance() is not declared in " + getClass().getSimpleName() + ". " +
                    "You should probably override getObjectType() in order to specify bean type.");
        }

        return type = (Class<T>) returnType;
    }

}
