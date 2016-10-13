package io.cattle.platform.configitem.context.data.metadata.version2;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;

public class ContainerMetaDataVersion3 extends ContainerMetaData {
    public ContainerMetaDataVersion3(ContainerMetaData data) {
        super(data);
        if (this.name != null) {
            this.name = this.name.toLowerCase();
        }
        if (this.service_name != null) {
            this.service_name = this.service_name.toLowerCase();
        }
        if (this.stack_name != null) {
            this.stack_name = this.stack_name.toLowerCase();
        }
    }
}
