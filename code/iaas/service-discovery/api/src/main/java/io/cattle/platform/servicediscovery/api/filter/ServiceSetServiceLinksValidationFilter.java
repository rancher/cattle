package io.cattle.platform.servicediscovery.api.filter;

import static io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes.*;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ServiceSetServiceLinksValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService", "dnsService" };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceDiscoveryConstants.ACTION_SERVICE_SET_SERVICE_LINKS)) {
            validateServices(Long.valueOf(request.getId()), request);
        }

        Service service = objectManager.loadResource(Service.class, request.getId());
        if (service.getKind()
                .equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            List<? extends LoadBalancerServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject())
                    .withKey(
                    ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_LIST)
                    .asList(jsonMapper, LoadBalancerServiceLink.class);
            for (LoadBalancerServiceLink link : serviceLinks) {
                if (link.getPorts() != null) {
                    for (String port : link.getPorts()) {
                        // to validate the spec
                        new LoadBalancerTargetPortSpec(port);
                    }
                }
            }
        }

        return super.resourceAction(type, request, next);
    }

    private void validateServices(long serviceId, ApiRequest request) {
        List<? extends ServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject()).withKey(
                ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).asList(jsonMapper, ServiceLink.class);
        List<String> serviceIdAndLinkName = new ArrayList<>();

        if (serviceLinks != null) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            for (ServiceLink serviceLink : serviceLinks) {
                if (serviceIdAndLinkName.contains(serviceLink.getUuid())) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                            ServiceDiscoveryConstants.FIELD_SERVICE_ID + " and link name combination");
                }
                serviceIdAndLinkName.add(serviceLink.getUuid());
                Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
                if (service == null || consumedService == null) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                            ServiceDiscoveryConstants.FIELD_SERVICE_ID);
                }
                validateName(serviceLink.getName());
            }
        }
    }

    private void validateName(String linkName) {
       if(linkName != null && !linkName.isEmpty()){
            validateDNSPatternForName(linkName);
            //check length
            if (linkName.length() < 1) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MIN_LENGTH_EXCEEDED,
                        "name");
            }
            if (linkName.length() > 63) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MAX_LENGTH_EXCEEDED,
                        "name");
            }
        }
    }
}
