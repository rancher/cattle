package io.cattle.platform.core.addon;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadBalancerTargetInput {
    List<? extends String> ports = new ArrayList<>();
    Service service;

    public LoadBalancerTargetInput(Service service, ServiceConsumeMap serviceLink, JsonMapper jsonMapper) {
        this.service = service;
        this.ports = DataAccessor.fields(serviceLink).
                withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, String.class);
    }

    public LoadBalancerTargetInput() {
    }

    public void setPorts(List<? extends String> ports) {
        this.ports = ports;
    }

    public List<? extends String> getPorts() {
        return ports;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
