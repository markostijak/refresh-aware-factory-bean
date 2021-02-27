package com.github.mscode.beans.factory.refreshaware.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "beans.factory.refresh")
public class RefreshableBeanProperties {

    /**
     * Enable beans refreshing.
     */
    private boolean enabled = true;

    /**
     * Call refresh with specified cron.
     * <p>
     * If specified, it has priority over {@link #fixedDelay} and {@link #fixedRate}.
     */
    private String cron;

    /**
     * Call refresh with a fixed period between the end of the
     * last invocation and the start of the next.
     * <p>
     * If specified, it has priority over {@link #fixedRate}.
     */
    private Duration fixedDelay;

    /**
     * Call refresh with a fixed period between invocations.
     */
    private Duration fixedRate = Duration.ofMinutes(1);

    /**
     * Delay before the first execution.
     */
    private Duration initialDelay = Duration.ofMinutes(1);

}
