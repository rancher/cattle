package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

public class ServiceSetServiceLinksValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceDiscoveryConstants.ACTION_SERVICE_SET_SERVICE_LINKS)) {
            validateServices(Long.valueOf(request.getId()), request);
        }

        return super.resourceAction(type, request, next);
    }

    @SuppressWarnings("unchecked")
    private void validateServices(long serviceId, ApiRequest request) {
        Map<String, Long> newServiceLinks = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        Service service = objectManager.loadResource(Service.class, serviceId);
        for (Long consumedServiceId : newServiceLinks.values()) {
            Service consumedService = objectManager.loadResource(Service.class, consumedServiceId);
            if (service == null || consumedService == null
                    || !consumedService.getEnvironmentId().equals(service.getEnvironmentId())) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        ServiceDiscoveryConstants.FIELD_SERVICE_ID);
            }
        }
    }
}
