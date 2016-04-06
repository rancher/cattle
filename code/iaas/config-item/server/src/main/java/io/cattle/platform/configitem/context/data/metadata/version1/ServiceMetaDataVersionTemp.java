package io.cattle.platform.configitem.context.data.metadata.version1;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.core.constants.CommonStatesConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceMetaDataVersionTemp extends ServiceMetaData {
    List<String> containers = new ArrayList<>();

    public ServiceMetaDataVersionTemp(ServiceMetaData serviceData) {
        super(serviceData);
        setContainerUuids();
    }

    protected void setContainerUuids() {
        if (super.containers != null) {
            for (ContainerMetaData c: super.containers) {
                List<String> removedStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING);
                if (removedStates.contains(c.getState())) {
                    continue;
                }
                this.containers.add(c.getMetadataUuid());
            }
        }
    }

    public List<String> getContainers() {
        return this.containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    @Override
    public void setContainersObj(List<ContainerMetaData> containers) {
        super.setContainersObj(containers);
        setContainerUuids();
    }
}

