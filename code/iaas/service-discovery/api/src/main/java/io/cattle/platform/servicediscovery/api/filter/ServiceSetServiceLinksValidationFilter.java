package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

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
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceDiscoveryConstants.ACTION_SERVICE_SET_SERVICE_LINKS)) {
            validateServices(Long.valueOf(request.getId()), request);
        }

        return super.resourceAction(type, request, next);
    }

    private void validateServices(long serviceId, ApiRequest request) {
        List<? extends ServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject()).withKey(
                ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).asList(jsonMapper, ServiceLink.class);

        if (serviceLinks != null) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            for (ServiceLink serviceLink : serviceLinks) {
                Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
                if (service == null || consumedService == null
                        || !consumedService.getEnvironmentId().equals(service.getEnvironmentId())) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                            ServiceDiscoveryConstants.FIELD_SERVICE_ID);
                }
            }
        }
    }
}
