package com.github.mscode.beans.factory.refreshaware;

import com.github.mscode.beans.factory.refreshaware.configuration.RefreshableBeanAutoConfiguration;
import com.github.mscode.beans.factory.refreshaware.configuration.RefreshableBeanProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;

/**
 * Simple template factory implementation with refresh
 * support.
 * <p>
 * Subclasses are responsible for implementing the
 * abstract {@link #createInstance()} template method
 * to actually create the object to expose.
 * <p>
 * Subclasses are responsible for implementing the
 * abstract {@link #refreshInstance()} template method
 * to actually refresh exposed object.
 * Default implementation will delegate object creation
 * to {@link #createInstance()} template method.
 *
 * @param <T> the bean type exposed by this factory
 * @see #createInstance()
 * @see #refreshInstance()
 * @see #destroyInstance
 * @see #shouldRefresh()
 * @see #refresh()
 */
@Slf4j
abstract class RefreshAwareFactoryBean<T> implements FactoryBean<T>, InitializingBean, RefreshableBean, DisposableBean {

    private T instance;

    private final Duration beforeRefresh;

    private final Duration beforeDestroy;

    private final TaskScheduler scheduler;

    private boolean initialized = false;

    protected Clock clock = Clock.systemDefaultZone();

    /**
     * Creates RefreshAwareFactoryBean without async support
     */
    public RefreshAwareFactoryBean() {
        this(null);
    }

    /**
     * Creates RefreshAwareFactoryBean with async support
     *
     * @param scheduler provided task scheduler
     */
    public RefreshAwareFactoryBean(@Nullable TaskScheduler scheduler) {
        this(scheduler, Duration.ZERO, Duration.ZERO);
    }

    /**
     * Same as {@link #RefreshAwareFactoryBean(TaskScheduler)}
     * with additional configuration parameters
     *
     * @param scheduler     provided task scheduler
     * @param beforeRefresh delay before {@link #refreshInstance()} attempt
     * @param beforeDestroy delay before {@link #destroyInstance(Object)} attempt
     */
    public RefreshAwareFactoryBean(@Nullable TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        this.scheduler = scheduler;
        this.beforeRefresh = beforeRefresh;
        this.beforeDestroy = beforeDestroy;
    }

    protected void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Template method that subclasses must override to construct
     * the object returned by this factory.
     * <p>
     * Invoked on initialization of this FactoryBean.
     *
     * @return the object returned by this factory
     * @throws Exception in case of any error.
     */
    @NonNull
    protected abstract T createInstance() throws Exception;

    /**
     * Template method that subclasses should override in order to
     * reconstruct the object returned by this factory.
     * <p>
     * Invoked on refresh when {@link #shouldRefresh()} is true
     * <p>
     * Callback for refreshing a refreshable instance. Subclasses may
     * override this to recreate the previously created instance in
     * different manner. If new instance can't be created, then this method
     * should return @{code null}.
     * <p>
     * The default implementation will call {@link #createInstance()}.
     *
     * @return the recreated object or {@code null} if new instance can't be created
     * @throws Exception in case of any error. Exceptions will get logged.
     */
    @Nullable
    protected T refreshInstance() throws Exception {
        return createInstance();
    }

    /**
     * Template method that subclasses can override to construct
     * dummy object returned by this factory.
     * <p>Invoked on initialization of this FactoryBean in case of
     * failure. If no dummy object is provided an exception will
     * be thrown.
     *
     * @return the object returned by this factory
     */
    @Nullable
    protected T createDummyInstance() {
        return null;
    }

    /**
     * Template method that subclasses must override in order
     * to signal whether the new instance should be created or not.
     * <p>
     *
     * @return whether to call {@link #refreshInstance()} or not
     * @throws Exception in case of any error. Exceptions will get logged.
     */
    protected abstract boolean shouldRefresh() throws Exception;

    /**
     * Callback for destroying a refreshable instance. Subclasses may
     * override this to destroy the previously created instance.
     * <p>
     * The default implementation will try to call {@link AutoCloseable#close()}
     *
     * @param instance the refreshable instance currently in use
     * @throws Exception in case of any error. Exceptions will get logged.
     * @see #createInstance()
     */
    protected void destroyInstance(@NonNull T instance) throws Exception {
        if (instance instanceof AutoCloseable) {
            ((AutoCloseable) instance).close();
        }
    }

    /**
     * Expose current instance to subclasses.
     *
     * @throws IllegalStateException if instance not yet initialized.
     * @see #createInstance()
     */
    @NonNull
    protected final T getInstance() throws IllegalStateException {
        Assert.state(initialized, "Refreshable instance not initialized yet");
        return instance;
    }

    /**
     * Eagerly create the refreshable instance.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.instance = createInstance();
        } catch (Exception e) {
            T dummy = createDummyInstance();
            if (dummy != null) {
                log.warn(getClass().getSimpleName() + "#createInstance() failed, using dummy instance", e);
                this.instance = dummy;
            } else {
                log.error(getClass().getSimpleName() + "#createInstance() failed, aborting", e);
                throw new IllegalStateException(e);
            }
        }

        this.initialized = true;
    }

    /**
     * Destroy the refreshable instance, if any.
     *
     * @see #destroyInstance(Object)
     */
    @Override
    public final void destroy() {
        if (instance != null) {
            try {
                destroyInstance(instance);
            } catch (Exception e) {
                log.error("{}#destroyInstance() failed", getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Called by {@link RefreshableBeanAutoConfiguration} at rates defined in {@link RefreshableBeanProperties}.
     * Exceptions will get logged.
     */
    @Override
    public final void refresh() {
        try {
            if (shouldRefresh()) {
                if (scheduler == null) {
                    doRefresh();
                } else {
                    doRefreshAsync();
                }
            }
        } catch (Exception e) {
            log.error("{}#shouldRefresh() failed", getClass().getSimpleName(), e);
        }
    }

    private void doRefresh() {
        try {
            T newInstance = refreshInstance();
            if (newInstance != null) {
                T oldInstance = instance;
                instance = newInstance;
                if (oldInstance != null) {
                    try {
                        destroyInstance(oldInstance);
                    } catch (Exception e) {
                        log.error("{}#destroyInstance() failed", getClass().getSimpleName(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("{}#refreshInstance() failed", getClass().getSimpleName(), e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void doRefreshAsync() {
        scheduler.schedule(() -> {
            try {
                T newInstance = refreshInstance();
                if (newInstance != null) {
                    T oldInstance = instance;
                    instance = newInstance;
                    if (oldInstance != null) {
                        scheduler.schedule(() -> {
                            try {
                                destroyInstance(oldInstance);
                            } catch (Exception e) {
                                log.error("{}#destroyInstance() failed", getClass().getSimpleName(), e);
                            }
                        }, clock.instant().plus(beforeDestroy));
                    }
                }
            } catch (Exception e) {
                log.error("{}#refreshInstance() failed", getClass().getSimpleName(), e);
            }
        }, clock.instant().plus(beforeRefresh));
    }

}
