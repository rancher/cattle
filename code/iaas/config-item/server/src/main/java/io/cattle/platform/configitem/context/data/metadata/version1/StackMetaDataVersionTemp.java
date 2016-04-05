package io.cattle.platform.configitem.context.data.metadata.version1;

import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class StackMetaDataVersionTemp extends StackMetaData {
    List<String> services;

    @SuppressWarnings("unchecked")
    public StackMetaDataVersionTemp(StackMetaData stackData) {
        super(stackData);
        this.services = (List<String>) CollectionUtils.collect(super.services,
                TransformerUtils.invokerTransformer("getUuid"));
    }

    public List<String> getServices() {
        return this.services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
