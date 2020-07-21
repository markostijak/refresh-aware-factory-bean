package com.github.beans.factory.refreshaware;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public class YamlFileProxyRefreshAwareFactoryBean<T> extends FileProxyRefreshAwareFactoryBean<T> {

    private final Yaml yaml;
    private final Class<T> type;

    public YamlFileProxyRefreshAwareFactoryBean(Path filepath, Class<T> type) {
        this(filepath, type, null);
    }

    public YamlFileProxyRefreshAwareFactoryBean(Path filepath, Class<T> type, TaskScheduler scheduler) {
        this(filepath, type, scheduler, Duration.ZERO, Duration.ZERO);
    }

    public YamlFileProxyRefreshAwareFactoryBean(Path filepath, Class<T> type, TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        super(filepath, scheduler, beforeRefresh, beforeDestroy);
        this.type = Objects.requireNonNull(type);
        this.yaml = new Yaml();
    }

    @NonNull
    @Override
    protected T createInstance(@NonNull Path filepath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filepath, charset)) {
            return yaml.loadAs(reader, type);
        }
    }

    @NonNull
    @Override
    public Class<T> getObjectType() {
        return type;
    }

}
