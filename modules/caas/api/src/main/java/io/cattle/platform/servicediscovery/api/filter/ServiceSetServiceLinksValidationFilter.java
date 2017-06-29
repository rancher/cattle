package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;

public class ServiceSetServiceLinksValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public ServiceSetServiceLinksValidationFilter(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request, ActionHandler next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_SET_SERVICE_LINKS)) {
            validateServices(Long.valueOf(request.getId()), request);
        }

        return super.perform(name, obj, request, next);
    }

    private void validateServices(long serviceId, ApiRequest request) {
        List<? extends ServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject()).withKey(
                ServiceConstants.FIELD_SERVICE_LINKS).asList(ServiceLink.class);
        List<String> serviceIdAndLinkName = new ArrayList<>();

        if (serviceLinks != null) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            for (ServiceLink serviceLink : serviceLinks) {
                if (serviceIdAndLinkName.contains(serviceLink.getUuid())) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                            InstanceConstants.FIELD_SERVICE_ID + " and link name combination");
                }
                serviceIdAndLinkName.add(serviceLink.getUuid());
                Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
                if (service == null || consumedService == null) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                            InstanceConstants.FIELD_SERVICE_ID);
                }
                ServiceUtil.validateLinkName(serviceLink.getName());
            }
        }
    }
}
