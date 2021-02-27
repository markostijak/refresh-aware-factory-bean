package com.github.mscode.beans.factory.refreshaware;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@SuppressWarnings("all")
public class JsonFileProxyRefreshAwareFactoryBean<T> extends FileProxyRefreshAwareFactoryBean<T> {

    protected final JavaType type;

    protected final ObjectMapper objectMapper;

    public JsonFileProxyRefreshAwareFactoryBean(String filepath, Type type) {
        this(Path.of(filepath), type);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, Type type) {
        this(filepath, type, null);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, ObjectMapper objectMapper, Class<T> type) {
        this(filepath, objectMapper, type, null);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, Type type, TaskScheduler scheduler) {
        this(filepath, type, scheduler, Duration.ZERO, Duration.ZERO);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, ObjectMapper objectMapper, Class<T> type, TaskScheduler scheduler) {
        this(filepath, objectMapper, type, scheduler, Duration.ZERO, Duration.ZERO);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, Type type, TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        this(filepath, new ObjectMapper(), type, scheduler, beforeRefresh, beforeDestroy);
    }

    public JsonFileProxyRefreshAwareFactoryBean(Path filepath, ObjectMapper objectMapper, Type type, TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        super(filepath, scheduler, beforeRefresh, beforeDestroy);
        this.objectMapper = objectMapper;
        this.type = objectMapper.getTypeFactory().constructType(type);
    }

    @NonNull
    @Override
    protected T createInstance(@NonNull Path filepath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filepath, charset)) {
            return (T) objectMapper.readValue(reader, type);
        }
    }

    @NonNull
    @Override
    public Class<T> getObjectType() {
        return (Class<T>) type.getRawClass();
    }

}
