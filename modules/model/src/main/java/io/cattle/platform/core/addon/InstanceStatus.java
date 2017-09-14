package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.Map;

@Type(list = false)
public class InstanceStatus {

    String instanceUuid;
    String externalId;
    String primaryIpAddress;
    Map<String, Object> dockerInspect;

    public InstanceStatus() {
    }

    public InstanceStatus(String instanceUuid, String externalId) {
        this.instanceUuid = instanceUuid;
        this.externalId = externalId;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPrimaryIpAddress() {
        return primaryIpAddress;
    }

    public void setPrimaryIpAddress(String primaryIpAddress) {
        this.primaryIpAddress = primaryIpAddress;
    }

    public Map<String, Object> getDockerInspect() {
        return dockerInspect;
    }

    public void setDockerInspect(Map<String, Object> dockerInspect) {
        this.dockerInspect = dockerInspect;
    }

}
