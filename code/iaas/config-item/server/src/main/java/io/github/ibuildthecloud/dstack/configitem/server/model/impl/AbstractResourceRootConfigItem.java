package io.github.ibuildthecloud.dstack.configitem.server.model.impl;

import io.github.ibuildthecloud.dstack.configitem.server.resource.ResourceRoot;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;

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
