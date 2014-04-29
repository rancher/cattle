package io.cattle.platform.configitem.server.task;

import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.task.AbstractSingletonTask;

import javax.inject.Inject;

public abstract class AbstractItemSyncTask extends AbstractSingletonTask {

    ConfigItemStatusManager statusManager;

    @Override
    protected void doRun() {
        statusManager.sync(isMigration());
    }

    protected abstract boolean isMigration();

    public ConfigItemStatusManager getStatusManager() {
        return statusManager;
    }

    @Inject
    public void setStatusManager(ConfigItemStatusManager statusManager) {
        this.statusManager = statusManager;
    }

}