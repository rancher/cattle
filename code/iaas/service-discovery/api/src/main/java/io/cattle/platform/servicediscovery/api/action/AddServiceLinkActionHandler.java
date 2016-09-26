package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.service.ServiceDiscoveryApiService;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AddServiceLinkActionHandler implements ActionHandler {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryApiService sdService;

    @Override
    public String getName() {
        return ServiceConstants.PROCESS_SERVICE_ADD_SERVICE_LINK;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            LoadBalancerServiceLink serviceLink = DataAccessor.fromMap(request.getRequestObject()).withKey(
                    ServiceConstants.FIELD_SERVICE_LINK).as(jsonMapper, LoadBalancerServiceLink.class);

            if (serviceLink.getPorts() != null) {
                for (String port : serviceLink.getPorts()) {
                    // to validate the spec
                    new LoadBalancerTargetPortSpec(port);
                }
            }

            sdService.addLoadBalancerServiceLink(service, serviceLink);
        } else {
            ServiceLink serviceLink = DataAccessor.fromMap(request.getRequestObject()).withKey(
                    ServiceConstants.FIELD_SERVICE_LINK).as(jsonMapper, ServiceLink.class);

            sdService.addServiceLink(service, serviceLink);
        }

        return service;
    }
}
