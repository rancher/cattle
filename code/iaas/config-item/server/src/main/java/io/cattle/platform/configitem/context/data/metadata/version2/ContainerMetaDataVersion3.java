package io.cattle.platform.configitem.context.data.metadata.version2;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ContainerMetaDataVersion3 extends ContainerMetaData {

    @SuppressWarnings("unchecked")
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
        List<String> portsObj = DataAccessor.fields(this.getInstance())
                .withKey(InstanceConstants.FIELD_PORTS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
        List<String> newPorts = new ArrayList<>();

        for (String portObj : portsObj) {
            PortSpec port = new PortSpec(portObj);
            if (StringUtils.isEmpty(port.getIpAddress())) {
                newPorts.add("0.0.0.0:" + portObj);
            } else {
                newPorts.add(portObj);
            }
        }
        this.ports = newPorts;
    }
}
