package io.github.ibuildthecloud.dstack.configitem.server.model.impl;

import io.github.ibuildthecloud.dstack.configitem.server.model.ConfigItem;
import io.github.ibuildthecloud.dstack.configitem.server.model.Request;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;

public abstract class AbstractConfigItem implements ConfigItem {

    String name;
    ConfigItemStatusManager versionManager;

    public AbstractConfigItem(String name, ConfigItemStatusManager versionManager) {
        super();
        this.name = name;
        this.versionManager = versionManager;
    }

    protected String getVersion(Request req) {
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigItemStatusManager getVersionManager() {
        return versionManager;
    }

    public void setVersionManager(ConfigItemStatusManager versionManager) {
        this.versionManager = versionManager;
    }

}
