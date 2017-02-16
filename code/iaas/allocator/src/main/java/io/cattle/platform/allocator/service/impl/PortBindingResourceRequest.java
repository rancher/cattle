package io.cattle.platform.allocator.service.impl;

import io.cattle.platform.core.util.PortSpec;

import java.util.List;

public class PortBindingResourceRequest implements ResourceRequest {
    private String instanceId;
    private String resourceUuid;
    private String resource;
    private List<PortSpec> portRequests;
    private String type;

    public List<PortSpec> getPortRequests() {
        return portRequests;
    }

    public void setPortRequests(List<PortSpec> portRequests) {
        this.portRequests = portRequests;
    }

    @Override
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("ports: %s", portRequests);
    }
}
