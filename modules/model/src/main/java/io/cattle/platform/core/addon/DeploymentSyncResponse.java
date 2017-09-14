package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.ArrayList;
import java.util.List;

@Type(list = false)
public class DeploymentSyncResponse {

    String nodeName;
    List<InstanceStatus> instanceStatus = new ArrayList<>();
    String externalId;

    public List<InstanceStatus> getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(List<InstanceStatus> instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getNodeName() {
        return nodeName;
    }

}
