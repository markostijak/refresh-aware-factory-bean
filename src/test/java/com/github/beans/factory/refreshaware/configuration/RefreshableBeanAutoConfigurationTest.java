package com.github.beans.factory.refreshaware.configuration;

import com.github.beans.factory.refreshaware.ProxyBasedRefreshAwareFactoryBean;
import com.github.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EnableScheduling
@EnableAutoConfiguration
@SpringBootConfiguration
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "beans.factory.refresh.initial-delay=500ms"
})
class RefreshableBeanAutoConfigurationTest {

    @Autowired
    private Model model;

    @Test
    void refresh() throws Exception {
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());

        TimeUnit.MILLISECONDS.sleep(800);

        assertEquals("refreshed model", model.getName());
        assertEquals(2, model.getVersion());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestProxyBasedRefreshAwareFactoryBean
            extends ProxyBasedRefreshAwareFactoryBean<Model> {

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
