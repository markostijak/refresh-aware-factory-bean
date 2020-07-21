package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyBasedRefreshAwareFactoryBeanTest {

    @Test
    void getObjectType() {
        RefreshAwareFactoryBean<Model> factoryBean = new TestProxyBasedRefreshAwareFactoryBean();

        assertEquals(Model.class, factoryBean.getObjectType());
    }

    @Test
    void lifeCycle() throws Exception {
        ProxyBasedRefreshAwareFactoryBean<Model> factoryBean = new TestProxyBasedRefreshAwareFactoryBean();

        // initialize
        factoryBean.afterPropertiesSet();

        Model model = factoryBean.getObject();
        assertEquals("Model", model.getName());
        assertEquals(1, model.getVersion());

        factoryBean.refresh();
        assertEquals("Model", model.getName());
        assertEquals(2, model.getVersion());

        factoryBean.refresh();
        assertEquals("Model", model.getName());
        assertEquals(3, model.getVersion());
    }

    static class TestProxyBasedRefreshAwareFactoryBean extends ProxyBasedRefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        @Override
        protected Model createInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }

    }

}
