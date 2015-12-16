package io.cattle.platform.configitem.context.data.metadata.version2;

import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;

import java.util.List;

public class StackMetaDataVersion2 extends StackMetaData {

    public StackMetaDataVersion2(StackMetaData stackData) {
        super(stackData);

    }

    public List<ServiceMetaData> getServices() {
        return super.services;
    }

    public void setServices(List<ServiceMetaData> services) {
        super.services = services;
    }
}
