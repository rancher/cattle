package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;
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
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            validateServices(Long.valueOf(request.getId()), data, request.getAction());
        }

        return super.resourceAction(type, request, next);
    }

    @SuppressWarnings("unchecked")
    private void validateServices(long serviceId, Map<String, Object> data, String action) {
        List<Long> consumedServiceIds = (List<Long>) data.get(ServiceDiscoveryConstants.FIELD_SERVICE_IDS);
        Service service = objectManager.loadResource(Service.class, serviceId);
        for (Long consumedServiceId : consumedServiceIds) {
            Service consumedService = objectManager.loadResource(Service.class, consumedServiceId);
            if (service == null || consumedService == null
                    || !consumedService.getEnvironmentId().equals(service.getEnvironmentId())) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        ServiceDiscoveryConstants.FIELD_SERVICE_ID);
            }
        }
    }
}
