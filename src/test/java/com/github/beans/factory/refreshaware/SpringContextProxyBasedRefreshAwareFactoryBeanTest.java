package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBootConfiguration
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class SpringContextProxyBasedRefreshAwareFactoryBeanTest {

    @Autowired
    private Model model;

    @Autowired
    private ProxyBasedRefreshAwareFactoryBean<Model> factoryBean;

    @Test
    void model() {
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());
    }

    @Test
    void factoryBean() throws Exception {
        assertEquals(Model.class, factoryBean.getObjectType());

        Model model = factoryBean.getObject();
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());

        factoryBean.refresh();

        assertEquals("refreshed model", model.getName());
        assertEquals(2, model.getVersion());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestProxyBasedRefreshAwareFactoryBean extends ProxyBasedRefreshAwareFactoryBean<Model> {
        @Override
        protected Model createInstance() throws Exception {
            return new Model("model", 1);
        }

        @Override
        protected Model refreshInstance() throws Exception {
            return new Model("refreshed model", 2);
        }

        @Override
        protected boolean shouldRefresh() throws Exception {
            return true;
        }
    }

}
