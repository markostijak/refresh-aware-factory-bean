package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.data.DataModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class YamlFileProxyRefreshAwareFactoryBeanTest {

    @Test
    void getObjectType() {
        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new YamlFileProxyRefreshAwareFactoryBean<>(mock(Path.class), DataModel.class);

        assertEquals(DataModel.class, factoryBean.getObjectType());
    }

    @Test
    void createInstance() throws Exception {
        Path filepath = Files.createTempFile("data-model", "txt");
        //language=yaml
        Files.writeString(filepath, "name: \"model\"\nversion: 1");

        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new YamlFileProxyRefreshAwareFactoryBean<>(filepath, DataModel.class);

        DataModel model = factoryBean.createInstance();
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());

        Files.delete(filepath);
    }

    @Test
    void lifeCycle() throws Exception {
        Path filepath = Files.createTempFile("data-model", "txt");
        //language=yaml
        Files.writeString(filepath, "name: \"Model x\"\nversion: 1");

        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new YamlFileProxyRefreshAwareFactoryBean<>(filepath, DataModel.class);
        factoryBean.setMinimalFileAge(Duration.ZERO);

        // initialize
        factoryBean.afterPropertiesSet();

        DataModel model = factoryBean.getObject();
        assertEquals("Model x", model.getName());
        assertEquals(1, model.getVersion());

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("Model x", model.getName());
        assertEquals(1, model.getVersion());

        //language=yaml
        Files.writeString(filepath, "name: \"Model y\"\nversion: 2");
        sleep(1);

        // file content updated
        factoryBean.refresh();
        assertEquals("Model y", model.getName());
        assertEquals(2, model.getVersion());

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("Model y", model.getName());
        assertEquals(2, model.getVersion());

        // destroy
        factoryBean.destroy();

        Files.delete(filepath);
    }

}
