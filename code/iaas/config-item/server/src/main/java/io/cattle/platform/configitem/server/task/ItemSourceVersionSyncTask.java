package io.cattle.platform.configitem.server.task;

import javax.inject.Inject;

import io.cattle.platform.configitem.server.service.ConfigItemServer;
import io.cattle.platform.task.AbstractSingletonTask;

public class ItemSourceVersionSyncTask extends AbstractSingletonTask {

    ConfigItemServer configItemServer;

    @Override
    public String getName() {
        return "config.item.source.version.sync";
    }

    @Override
    protected void doRun() {
        configItemServer.syncSourceVersion();
    }

    public ConfigItemServer getConfigItemServer() {
        return configItemServer;
    }

    @Inject
    public void setConfigItemServer(ConfigItemServer configItemServer) {
        this.configItemServer = configItemServer;
    }

}
