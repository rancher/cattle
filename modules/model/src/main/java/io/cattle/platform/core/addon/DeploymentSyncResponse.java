package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.ArrayList;
import java.util.List;

@Type(list = false)
public class DeploymentSyncResponse {

    List<InstanceStatus> instanceStatus = new ArrayList<>();

    public List<InstanceStatus> getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(List<InstanceStatus> instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

}
