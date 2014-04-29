package io.cattle.platform.configitem.server.model.impl;

import java.io.IOException;

import io.cattle.platform.configitem.server.model.RefreshableConfigItem;
import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

public abstract class AbstractResourceRootConfigItem extends AbstractConfigItem implements RefreshableConfigItem {

    ResourceRoot resourceRoot;

    public AbstractResourceRootConfigItem(String name, ConfigItemStatusManager versionManager, ResourceRoot resourceRoot) {
        super(name, versionManager);
        this.resourceRoot = resourceRoot;
    }

    @Override
    public String getSourceRevision() {
        return resourceRoot.getSourceRevision();
    }

    @Override
    public boolean refresh() throws IOException {
        return resourceRoot.scan();
    }

    public ResourceRoot getResourceRoot() {
        return resourceRoot;
    }

}
