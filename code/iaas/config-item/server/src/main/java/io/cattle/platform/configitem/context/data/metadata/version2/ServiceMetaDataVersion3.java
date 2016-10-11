package io.cattle.platform.configitem.context.data.metadata.version2;

import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;

public class ServiceMetaDataVersion3 extends ServiceMetaDataVersion2 {

    public ServiceMetaDataVersion3(ServiceMetaData serviceData) {
        super(serviceData);
        if (this.name != null) {
            this.name = this.name.toLowerCase();
        }
        if (this.stack_name != null) {
            this.stack_name = this.stack_name.toLowerCase();
        }
    }
}
