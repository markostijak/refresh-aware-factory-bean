package com.github.beans.factory.refreshaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.beans.factory.refreshaware.data.DataModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JsonFileProxyRefreshAwareFactoryBeanTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getObjectType() {
        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new JsonFileProxyRefreshAwareFactoryBean<>(mock(Path.class), objectMapper, DataModel.class);

        assertEquals(DataModel.class, factoryBean.getObjectType());
    }

    @Test
    void createInstance() throws Exception {
        Path filepath = Files.createTempFile("data-model", "txt");
        //language=json
        Files.writeString(filepath, "{\"name\": \"model\", \"version\": 1}");

        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new JsonFileProxyRefreshAwareFactoryBean<>(filepath, objectMapper, DataModel.class);

        DataModel model = factoryBean.createInstance();
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());

        Files.delete(filepath);
    }

    @Test
    void lifeCycle() throws Exception {
        Path filepath = Files.createTempFile("data-model", "txt");
        //language=json
        Files.writeString(filepath, "{\"name\": \"Model x\", \"version\": 1}");

        FileProxyRefreshAwareFactoryBean<DataModel> factoryBean =
                new JsonFileProxyRefreshAwareFactoryBean<>(filepath, objectMapper, DataModel.class);
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

        //language=json
        Files.writeString(filepath, "{\"name\": \"Model y\", \"version\": 2}");
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
