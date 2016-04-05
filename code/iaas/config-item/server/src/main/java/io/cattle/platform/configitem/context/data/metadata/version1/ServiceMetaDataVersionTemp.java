package io.cattle.platform.configitem.context.data.metadata.version1;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class ServiceMetaDataVersionTemp extends ServiceMetaData {
    List<String> containers = new ArrayList<>();

    public ServiceMetaDataVersionTemp(ServiceMetaData serviceData) {
        super(serviceData);
        setContainerUuids();
    }

    @SuppressWarnings("unchecked")
    protected void setContainerUuids() {
        if (super.containers != null) {
            this.containers = (List<String>) CollectionUtils.collect(super.containers,
                    TransformerUtils.invokerTransformer("getUuid"));
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

