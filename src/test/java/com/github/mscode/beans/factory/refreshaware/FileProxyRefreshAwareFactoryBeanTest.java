package com.github.mscode.beans.factory.refreshaware;

import com.github.mscode.beans.factory.refreshaware.data.Model;
import com.github.mscode.beans.factory.refreshaware.tools.clock.MutableClock;
import com.github.mscode.beans.factory.refreshaware.tools.filesystem.EnableInMemoryFileSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableInMemoryFileSystem
@ExtendWith(SpringExtension.class)
class FileProxyRefreshAwareFactoryBeanTest {

    @Autowired
    private FileSystem fileSystem;

    private MutableClock clock;

    private Path filepath;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(1600000000);
        filepath = fileSystem.getPath("model.txt");
    }

    @Test
    void getObjectType() {
        FileProxyRefreshAwareFactoryBean<Model> factoryBean =
                new TestFileProxyRefreshAwareFactoryBean(filepath);

        assertEquals(Model.class, factoryBean.getObjectType());
    }

    @Test
    public void shouldRefresh_missingFile() throws Exception {
        FileProxyRefreshAwareFactoryBean<Model> factoryBean =
                new TestFileProxyRefreshAwareFactoryBean(filepath);

        // initialize
        factoryBean.afterPropertiesSet();

        assertThrows(NoSuchFileException.class, factoryBean::shouldRefresh);
    }

    @Test
    void shouldRefresh() throws Exception {
        Files.writeString(filepath, "created");
        Files.setLastModifiedTime(filepath, FileTime.from(clock.instant()));

        FileProxyRefreshAwareFactoryBean<Model> factoryBean =
                new TestFileProxyRefreshAwareFactoryBean(filepath);
        factoryBean.setMinimalFileAge(Duration.ofSeconds(3));
        factoryBean.setClock(clock);

        factoryBean.afterPropertiesSet();

        // file is same as previous
        assertFalse(factoryBean.shouldRefresh());

        clock.tickSeconds(2);

        Files.writeString(filepath, "updated");
        Files.setLastModifiedTime(filepath, FileTime.from(clock.instant()));

        clock.tickSeconds(2);

        // prevented by minimal file age
        assertFalse(factoryBean.shouldRefresh());

        clock.tickSeconds(2);

        assertTrue(factoryBean.shouldRefresh());
    }

    @Test
    void lifeCycle() throws Exception {
        Files.writeString(filepath, "created");
        Files.setLastModifiedTime(filepath, FileTime.from(clock.instant()));

        FileProxyRefreshAwareFactoryBean<Model> factoryBean =
                new TestFileProxyRefreshAwareFactoryBean(filepath);
        factoryBean.setMinimalFileAge(Duration.ofSeconds(1));
        factoryBean.setClock(clock);

        // initialize
        factoryBean.afterPropertiesSet();

        Model model = factoryBean.getObject();
        assertEquals("created", model.getName());
        assertEquals(1, model.getVersion());

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("created", model.getName());
        assertEquals(1, model.getVersion());

        clock.tickSeconds(2);

        Files.writeString(filepath, "updated");
        Files.setLastModifiedTime(filepath, FileTime.from(clock.instant()));

        clock.tickSeconds(2);

        // file content updated
        factoryBean.refresh();
        assertEquals("updated", model.getName());
        assertEquals(2, model.getVersion());

        clock.tickSeconds(2);

        // file content is same as previous
        factoryBean.refresh();
        assertEquals("updated", model.getName());
        assertEquals(2, model.getVersion());

        // destroy
        factoryBean.destroy();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(filepath)) {
            Files.delete(filepath);
        }
    }

    static class TestFileProxyRefreshAwareFactoryBean
            extends FileProxyRefreshAwareFactoryBean<Model> {

        private static int counter = 1;

        public TestFileProxyRefreshAwareFactoryBean(Path filepath) {
            super(filepath);
        }

        @Override
        protected Model createInstance(Path filepath) throws Exception {
            return new Model(Files.readString(filepath), counter++);
        }

        @Override
        protected Model createDummyInstance() {
            return new Model("dummy model", 0);
        }
    }

}
