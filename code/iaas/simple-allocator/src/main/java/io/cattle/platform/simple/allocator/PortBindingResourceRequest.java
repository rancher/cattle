package io.cattle.platform.simple.allocator;

import io.cattle.platform.core.util.PortSpec;

import java.util.List;

public class PortBindingResourceRequest implements ResourceRequest {
    private String instanceId;
    private String resource;
    private List<PortSpec> PortRequests;
    private String type;

    public List<PortSpec> getPortRequests() {
        return PortRequests;
    }

    public void setPortRequests(List<PortSpec> portRequests) {
        PortRequests = portRequests;
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

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
