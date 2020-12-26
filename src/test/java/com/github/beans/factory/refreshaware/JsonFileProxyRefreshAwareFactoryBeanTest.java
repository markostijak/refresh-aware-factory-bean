package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.data.DataModel;
import com.github.beans.factory.refreshaware.tools.filesystem.EnableInMemoryFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@EnableInMemoryFileSystem
@ExtendWith(SpringExtension.class)
class JsonFileProxyRefreshAwareFactoryBeanTest {

    @Autowired
    private FileSystem fileSystem;

    @Test
    void getObjectType() {
        RefreshAwareFactoryBean<DataModel> factoryBean =
                new JsonFileProxyRefreshAwareFactoryBean<>(mock(Path.class), DataModel.class);

        assertEquals(DataModel.class, factoryBean.getObjectType());
    }

    @Test
    void createInstance() throws Exception {
        Path filepath = fileSystem.getPath("data-model.json");

        //language=json
        Files.writeString(filepath, "{\"name\": \"model\", \"version\": 1}");

        RefreshAwareFactoryBean<DataModel> factoryBean =
                new JsonFileProxyRefreshAwareFactoryBean<>(filepath, DataModel.class);

        DataModel model = factoryBean.createInstance();
        assertEquals("model", model.getName());
        assertEquals(1, model.getVersion());

        Files.delete(filepath);
    }

}
