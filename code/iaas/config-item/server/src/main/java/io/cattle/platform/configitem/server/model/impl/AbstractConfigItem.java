package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

public abstract class AbstractConfigItem implements ConfigItem {

    String name;
    ConfigItemStatusManager versionManager;

    public AbstractConfigItem(String name, ConfigItemStatusManager versionManager) {
        super();
        this.name = name;
        this.versionManager = versionManager;
    }

    protected String getVersion(Request req) {
        ItemVersion version = versionManager.getRequestedVersion(req.getClient(), req.getItemName());
        return version == null ? null : version.toExternalForm();
    }

    @Override
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
