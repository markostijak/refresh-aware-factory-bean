package com.github.mscode.beans.factory.refreshaware.tools.filesystem;

import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in unit testing when file operations are needed.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ContextConfiguration(classes = InMemoryFileSystemConfiguration.class)
public @interface EnableInMemoryFileSystem {
}
