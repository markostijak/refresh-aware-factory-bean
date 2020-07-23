package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FileProxyRefreshAwareFactoryBeanTest {

    @Test
    void getObjectType() {
        RefreshAwareFactoryBean<Model> factoryBean =
                new TestFileProxyRefreshAwareFactoryBean(mock(Path.class));

        assertEquals(Model.class, factoryBean.getObjectType());
    }

    @Test
    void shouldRefresh() throws Exception {
        Path filepath = Files.createTempFile("model", "txt");
        Files.writeString(filepath, "created");

        FileProxyRefreshAwareFactoryBean<Model> factoryBean = new TestFileProxyRefreshAwareFactoryBean(filepath);
        factoryBean.setMinimalFileAge(Duration.ZERO);

        factoryBean.afterPropertiesSet();

        assertFalse(factoryBean.shouldRefresh());

        Files.writeString(filepath, "updated");

        sleep(1);

        assertTrue(factoryBean.shouldRefresh());

        Files.delete(filepath);
    }

    @Test
    void lifeCycle() throws Exception {
        Path filepath = Files.createTempFile("model", "txt");
        Files.writeString(filepath, "created");

        FileProxyRefreshAwareFactoryBean<Model> factoryBean = new TestFileProxyRefreshAwareFactoryBean(filepath);
        factoryBean.setMinimalFileAge(Duration.ZERO);

        // initialize
        factoryBean.afterPropertiesSet();

        Model model = factoryBean.getObject();
        assertEquals("created", model.getName());
        assertEquals(1, model.getVersion());

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("created", model.getName());
        assertEquals(1, model.getVersion());

        Files.writeString(filepath, "updated");
        sleep(1);

        // file content updated
        factoryBean.refresh();
        assertEquals("updated", model.getName());
        assertEquals(2, model.getVersion());

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("updated", model.getName());
        assertEquals(2, model.getVersion());

        // destroy
        factoryBean.destroy();

        Files.delete(filepath);
    }

    static class TestFileProxyRefreshAwareFactoryBean extends FileProxyRefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        public TestFileProxyRefreshAwareFactoryBean(Path filepath) {
            super(filepath);
        }

        @Override
        protected Model createInstance(Path filepath) throws Exception {
            return new Model(Files.readString(filepath), counter++);
        }

    }

}
