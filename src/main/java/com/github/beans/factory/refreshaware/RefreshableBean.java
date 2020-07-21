package com.github.beans.factory.refreshaware;

import com.github.beans.factory.refreshaware.configuration.RefreshableBeanProperties;

/**
 * Interface intended to be implemented by beans that wants to refresh current state.
 * <p>
 * Refreshing works in combination with {@link org.springframework.scheduling.annotation.EnableScheduling},
 * and if it is enabled, {@link RefreshableBean#refresh()} will be invoked on every bean implementing this interface
 * at rate specified in {@link RefreshableBeanProperties}.
 */
public interface RefreshableBean {

    /**
     * Invoked at rates defined in {@link RefreshableBeanProperties}.
     *
     * @throws Exception in case of refreshing error. Exceptions will get logged
     *                   but not rethrown to allow other beans to refresh their state as well.
     */
    void refresh() throws Exception;

}
