package io.cattle.platform.configitem.context.data.metadata.version1;

import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class ServiceMetaDataVersion1 extends ServiceMetaData {
    List<String> containers = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ServiceMetaDataVersion1(ServiceMetaData serviceData) {
        super(serviceData);
        this.containers = (List<String>) CollectionUtils.collect(super.containers,
                TransformerUtils.invokerTransformer("getName"));
    }

    public List<String> getContainers() {
        return this.containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }
}
