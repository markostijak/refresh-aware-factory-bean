package com.github.beans.factory.refreshaware;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Convenient factory bean for creating beans dependent
 * on file content and refreshing beans on file modification.
 *
 * @param <T> type of object that this factory bean creates
 *            wrapped in {@link org.springframework.aop.framework.AopProxy}
 */
@SuppressWarnings("all")
public abstract class FileProxyRefreshAwareFactoryBean<T> extends ProxyBasedRefreshAwareFactoryBean<T> {

    private Path filepath;

    private FileTime lastModified;

    private Duration minimalFileAge = Duration.ofSeconds(1);

    protected Charset charset = StandardCharsets.UTF_8;

    public FileProxyRefreshAwareFactoryBean(@NonNull String filepath) {
        this(Path.of(filepath));
    }

    public FileProxyRefreshAwareFactoryBean(@NonNull Path filepath) {
        this(filepath, null);
    }

    public FileProxyRefreshAwareFactoryBean(@NonNull Path filepath, TaskScheduler scheduler) {
        this(filepath, scheduler, Duration.ZERO, Duration.ZERO);
    }

    public FileProxyRefreshAwareFactoryBean(@NonNull Path filepath, TaskScheduler scheduler, Duration beforeRefresh, Duration beforeDestroy) {
        super(scheduler, beforeRefresh, beforeDestroy);
        this.filepath = Objects.requireNonNull(filepath);
    }

    public void setMinimalFileAge(Duration age) {
        this.minimalFileAge = age;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if (Files.exists(filepath)) {
            this.lastModified = Files.getLastModifiedTime(filepath);
        } else {
            this.lastModified = FileTime.fromMillis(Long.MIN_VALUE);
        }
    }

    @NonNull
    @Override
    protected final T createInstance() throws Exception {
        return createInstance(filepath);
    }

    @Override
    protected final T refreshInstance() throws Exception {
        return refreshInstance(filepath);
    }

    /**
     * Detects file modification and signals for refresh attempt.
     *
     * @return {@code true} if file is modified
     * @throws Exception in case of any error
     */
    @Override
    protected boolean shouldRefresh() throws Exception {
        FileTime newTime = Files.getLastModifiedTime(filepath);
        if (newTime.compareTo(lastModified) > 0) {
            Instant now = clock.instant();
            if (now.isAfter(newTime.toInstant().plus(minimalFileAge))) {
                this.lastModified = newTime;
                return true;
            }
        }

        return false;
    }

    /**
     * Convinient template method which creates
     * new instance based on specified file.
     *
     * @param filepath file
     * @return new instace
     * @throws Exception in case of any error.
     */
    @NonNull
    protected abstract T createInstance(@NonNull Path filepath) throws Exception;

    /**
     * Convinient template method which recreates
     * new instance based on specified file.
     *
     * @param filepath file
     * @return newly created instace or {@code null} if new instance can't be created
     * @throws Exception in case of any error.
     */
    @Nullable
    protected T refreshInstance(@NonNull Path filepath) throws Exception {
        return createInstance(filepath);
    }

}
