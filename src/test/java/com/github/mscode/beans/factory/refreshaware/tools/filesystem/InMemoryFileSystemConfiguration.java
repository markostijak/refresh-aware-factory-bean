package com.github.mscode.beans.factory.refreshaware.tools.filesystem;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.time.temporal.ChronoUnit;

/**
 * Create specific in memory file system
 * for usage in unit testing.
 * <p>
 * {@link @Lazy} is used to create only needed FS
 * implementation.
 */
@Lazy
@TestConfiguration
public class InMemoryFileSystemConfiguration {

   @Bean
   @Primary
   @ConditionalOnClass(MemoryFileSystemBuilder.class)
   public FileSystem fileSystem() throws IOException {
      return MemoryFileSystemBuilder.newEmpty()
            .setFileTimeResolution(ChronoUnit.NANOS)
            .build();
   }

   @Bean("windows")
   @ConditionalOnClass(MemoryFileSystemBuilder.class)
   public FileSystem windowsFileSystem() throws IOException {
      return MemoryFileSystemBuilder.newWindows()
            .setFileTimeResolution(ChronoUnit.NANOS)
            .build();
   }

   @Bean("linux")
   @ConditionalOnClass(MemoryFileSystemBuilder.class)
   public FileSystem linuxFileSystem() throws IOException {
      return MemoryFileSystemBuilder.newLinux()
            .setFileTimeResolution(ChronoUnit.NANOS)
            .build();
   }

}
