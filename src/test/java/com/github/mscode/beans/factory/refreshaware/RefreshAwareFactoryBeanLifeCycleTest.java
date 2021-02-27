package com.github.mscode.beans.factory.refreshaware;

import com.github.mscode.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RefreshAwareFactoryBeanLifeCycleTest {

    @Test
    void lifeCycle_regular() throws Exception {
        RefreshAwareFactoryBean<Model> factoryBean =
                spy(new RegularRefreshAwareFactoryBean());

        // initialize
        factoryBean.afterPropertiesSet();
        verify(factoryBean, times(1)).createInstance();
        verify(factoryBean, times(0)).createDummyInstance();

        Model model1 = factoryBean.getObject();
        assertNotNull(model1);
        assertEquals("Model", model1.getName());
        assertEquals(1, model1.getVersion());

        // refresh - #1
        factoryBean.refresh();
        verify(factoryBean, times(1)).shouldRefresh();
        verify(factoryBean, times(1)).refreshInstance();
        verify(factoryBean, times(1)).destroyInstance(model1);

        Model model2 = factoryBean.getObject();
        assertNotNull(model2);
        assertEquals("Model", model2.getName());
        assertEquals(2, model2.getVersion());

        // refresh - #2
        factoryBean.refresh();
        verify(factoryBean, times(2)).shouldRefresh();
        verify(factoryBean, times(2)).refreshInstance();
        verify(factoryBean, times(1)).destroyInstance(model2);

        Model model3 = factoryBean.getObject();
        assertNotNull(model3);
        assertEquals("Model", model3.getName());
        assertEquals(3, model3.getVersion());

        // destroy
        factoryBean.destroy();
        verify(factoryBean, times(1)).destroyInstance(model3);
    }

    @Test
    void lifeCycle_broken_useDummyInstance() throws Exception {
        RefreshAwareFactoryBean<Model> factoryBean =
                spy(new BrokenRefreshAwareFactoryBean());

        // initialize
        factoryBean.afterPropertiesSet();
        verify(factoryBean, times(1)).createInstance();
        verify(factoryBean, times(1)).createDummyInstance();

        Model model0 = factoryBean.getObject();
        assertNotNull(model0);
        assertEquals("Model", model0.getName());
        assertEquals(0, model0.getVersion());

        // refresh - #1
        factoryBean.refresh();
        verify(factoryBean, times(1)).shouldRefresh();
        verify(factoryBean, times(1)).refreshInstance();
        verify(factoryBean, times(1)).destroyInstance(model0);

        Model model2 = factoryBean.getObject();
        assertNotNull(model2);
        assertEquals("Model", model2.getName());
        assertEquals(2, model2.getVersion());

        // refresh - #2
        factoryBean.refresh();
        verify(factoryBean, times(2)).shouldRefresh();
        verify(factoryBean, times(2)).refreshInstance();
        verify(factoryBean, times(1)).destroyInstance(model2);

        Model model3 = factoryBean.getObject();
        assertNotNull(model3);
        assertEquals("Model", model3.getName());
        assertEquals(3, model3.getVersion());

        // destroy
        factoryBean.destroy();
        verify(factoryBean, times(1)).destroyInstance(model3);
    }

    @Test
    void lifeCycle_broken_abort() throws Exception {
        RefreshAwareFactoryBean<Model> factoryBean =
                spy(new AbortRefreshAwareFactoryBean());

        // initialize
        assertThrows(Exception.class, factoryBean::afterPropertiesSet);
        verify(factoryBean, times(1)).createInstance();
        verify(factoryBean, times(1)).createDummyInstance();
    }

    @Test
    void lifeCycle_broken_refreshAttemptFailed() throws Exception {
        RefreshAwareFactoryBean<Model> factoryBean =
                spy(new RefreshAttemptFailedRefreshAwareFactoryBean());

        // initialize
        factoryBean.afterPropertiesSet();
        verify(factoryBean, times(1)).createInstance();
        verify(factoryBean, times(0)).createDummyInstance();

        Model model1 = factoryBean.getObject();
        assertNotNull(model1);
        assertEquals("Model", model1.getName());
        assertEquals(1, model1.getVersion());

        // refresh failed
        factoryBean.refresh();
        verify(factoryBean, times(1)).shouldRefresh();
        verify(factoryBean, times(1)).refreshInstance();
        verify(factoryBean, times(0)).destroyInstance(model1);

        Model model2 = factoryBean.getObject();
        assertEquals(model1, model2);

        // destroy
        factoryBean.destroy();
        verify(factoryBean, times(1)).destroyInstance(model1);
    }

    /**
     * Regular state, createInstance is able to create bean instance.
     */
    static class RegularRefreshAwareFactoryBean
            extends RefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        @Override
        protected Model createInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected Model refreshInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }

        @Override
        public Model getObject() throws Exception {
            return getInstance();
        }

        @Override
        public Class<?> getObjectType() {
            return Model.class;
        }

    }

    /**
     * Unable to create instance, createInstance failed,
     * but createDummyInstance is implemented and provides default bean instance (state).
     */
    static class BrokenRefreshAwareFactoryBean
            extends RefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        @Override
        protected Model createInstance() throws Exception {
            counter++;
            throw new Exception("Simulate initialization error...");
        }

        @Override
        protected Model createDummyInstance() {
            return new Model("Model", 0);
        }

        @Override
        protected Model refreshInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }

        @Override
        public Model getObject() throws Exception {
            return getInstance();
        }

        @Override
        public Class<?> getObjectType() {
            return Model.class;
        }

    }

    /**
     * Unable to create instance, createInstance failed,
     * and createDummyInstance is not implemented.
     */
    static class AbortRefreshAwareFactoryBean
            extends RefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        @Override
        protected Model createInstance() throws Exception {
            throw new Exception("Simulate initialization error...");
        }

        @Override
        protected Model refreshInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }

        @Override
        public Model getObject() throws Exception {
            return getInstance();
        }

        @Override
        public Class<?> getObjectType() {
            return Model.class;
        }

    }

    /**
     * Unable to refresh instance, shouldRefresh failed.
     */
    static class RefreshAttemptFailedRefreshAwareFactoryBean
            extends RefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        @Override
        protected Model createInstance() throws Exception {
            return new Model("Model", counter++);
        }

        @Override
        protected Model refreshInstance() throws Exception {
            throw new Exception("Simulate refresh attempt fail....");
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }

        @Override
        public Model getObject() throws Exception {
            return getInstance();
        }

        @Override
        public Class<?> getObjectType() {
            return Model.class;
        }

    }

}
