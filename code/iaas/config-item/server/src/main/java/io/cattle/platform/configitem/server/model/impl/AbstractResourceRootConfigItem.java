package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

public abstract class AbstractResourceRootConfigItem extends AbstractConfigItem {

    ResourceRoot resourceRoot;

    public AbstractResourceRootConfigItem(String name, ConfigItemStatusManager versionManager, ResourceRoot resourceRoot) {
        super(name, versionManager);
        this.resourceRoot = resourceRoot;
    }

    @Override
    public String getSourceRevision() {
        return resourceRoot.getSourceRevision();
    }

    public ResourceRoot getResourceRoot() {
        return resourceRoot;
    }
}
