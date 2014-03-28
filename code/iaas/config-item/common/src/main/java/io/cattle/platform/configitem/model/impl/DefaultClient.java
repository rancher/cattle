package io.cattle.platform.configitem.model.impl;

import io.cattle.platform.configitem.model.Client;

public class DefaultClient implements Client {

    Class<?> resourceType;
    long resourceId;

    public DefaultClient() {
    }

    public DefaultClient(Class<?> resourceType, long resourceId) {
        super();
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    @Override
    public Class<?> getResourceType() {
        return resourceType;
    }

    public void setResourceType(Class<?> resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        return (resourceType.getSimpleName() + ":" + resourceId).toLowerCase();
    }

}
