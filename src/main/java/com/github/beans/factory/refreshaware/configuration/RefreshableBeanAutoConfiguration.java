package com.github.beans.factory.refreshaware.configuration;

import com.github.beans.factory.refreshaware.RefreshableBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnBean(annotation = EnableScheduling.class)
@EnableConfigurationProperties(RefreshableBeanProperties.class)
public class RefreshableBeanAutoConfiguration implements InitializingBean {

    private final TaskScheduler scheduler;
    private final List<RefreshableBean> beans;
    private final RefreshableBeanProperties properties;

    @Autowired
    public RefreshableBeanAutoConfiguration(RefreshableBeanProperties properties,
                                            List<RefreshableBean> beans, TaskScheduler scheduler) {
        this.beans = beans;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.isEnabled()) {
            Trigger trigger = createTrigger();
            schedule(beans, scheduler, trigger);
        }
    }

    private void schedule(List<RefreshableBean> beans, TaskScheduler scheduler, Trigger trigger) {
        if (!CollectionUtils.isEmpty(beans)) {
            scheduler.schedule(() -> callRefresh(beans), trigger);
        }
    }

    private void callRefresh(List<RefreshableBean> beans) {
        for (RefreshableBean bean : beans) {
            try {
                bean.refresh();
            } catch (Exception e) {
                log.error("{}#refresh() failed.", beans.getClass().getSimpleName(), e);
            }
        }
    }

    private Trigger createTrigger() {
        if (properties.getCron() != null) {
            return new CronTrigger(properties.getCron());
        }

        PeriodicTrigger trigger;

        if (properties.getFixedDelay() != null) {
            trigger = new PeriodicTrigger(properties.getFixedDelay().toMillis());
        } else {
            trigger = new PeriodicTrigger(properties.getFixedRate().toMillis());
            trigger.setFixedRate(true);
        }

        trigger.setInitialDelay(properties.getInitialDelay().toMillis());

        return trigger;
    }

}
