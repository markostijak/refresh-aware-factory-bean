package com.github.mscode.beans.factory.refreshaware.configuration;

import com.github.mscode.beans.factory.refreshaware.ProxyBasedRefreshAwareFactoryBean;
import com.github.mscode.beans.factory.refreshaware.data.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RefreshableBeanAutoConfigurationTest {

    private AutoConfigurations autoConfigurations;
    private UserConfigurations userConfigurations;

    private PeriodicTrigger periodicTrigger;
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    public void setUp() {
        contextRunner = new ApplicationContextRunner();

        autoConfigurations = AutoConfigurations.of(
                RefreshableBeanAutoConfiguration.class
        );

        userConfigurations = UserConfigurations.of(
                TaskSchedulerConfiguration.class,
                TestProxyBasedRefreshAwareFactoryBean.class
        );

        periodicTrigger = new PeriodicTrigger(1);
        periodicTrigger.setInitialDelay(2);
        periodicTrigger.setFixedRate(true);
    }

    @Test
    public void refresh() {
        contextRunner.withConfiguration(userConfigurations)
                .withConfiguration(autoConfigurations)
                .withPropertyValues("beans.factory.refresh.fixed-rate: 1ms")
                .withPropertyValues("beans.factory.refresh.initial-delay: 2ms")
                .run(context -> {
                    assertThat(context).hasSingleBean(TaskScheduler.class);
                    assertThat(context).hasSingleBean(RefreshableBeanAutoConfiguration.class);

                    TaskScheduler taskScheduler = context.getBean(TaskScheduler.class);

                    Model model = context.getBean(Model.class);
                    assertEquals("model", model.getName());
                    assertEquals(1, model.getVersion());

                    // call scheduler
                    context.publishEvent(mock(ApplicationStartedEvent.class));
                    verify(taskScheduler, times(1)).schedule(any(Runnable.class), eq(periodicTrigger));

                    assertEquals("refreshed model", model.getName());
                    assertEquals(2, model.getVersion());
                });
    }

    @TestComponent
    static class TestProxyBasedRefreshAwareFactoryBean
            extends ProxyBasedRefreshAwareFactoryBean<Model> {

        @NonNull
        @Override
        protected Model createInstance() {
            return new Model("model", 1);
        }

        @Override
        protected Model refreshInstance() {
            return new Model("refreshed model", 2);
        }

        @Override
        protected boolean shouldRefresh() {
            return true;
        }
    }

    @EnableScheduling
    @TestConfiguration
    static class TaskSchedulerConfiguration {
        @Bean
        public TaskScheduler taskScheduler() {
            TaskScheduler taskScheduler = mock(TaskScheduler.class);
            when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).then(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            });
            return taskScheduler;
        }
    }

}
