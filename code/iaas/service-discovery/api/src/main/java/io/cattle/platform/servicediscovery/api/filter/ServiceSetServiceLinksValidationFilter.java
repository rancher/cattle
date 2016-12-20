package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
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
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_SET_SERVICE_LINKS)) {
            validateServices(Long.valueOf(request.getId()), request);
        }

        return super.resourceAction(type, request, next);
    }

    private void validateServices(long serviceId, ApiRequest request) {
        List<? extends ServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject()).withKey(
                ServiceConstants.FIELD_SERVICE_LINKS).asList(jsonMapper, ServiceLink.class);
        List<String> serviceIdAndLinkName = new ArrayList<>();

        if (serviceLinks != null) {
            Service service = objectManager.loadResource(Service.class, serviceId);
            for (ServiceLink serviceLink : serviceLinks) {
                if (serviceIdAndLinkName.contains(serviceLink.getUuid())) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                            ServiceConstants.FIELD_SERVICE_ID + " and link name combination");
                }
                serviceIdAndLinkName.add(serviceLink.getUuid());
                Service consumedService = objectManager.loadResource(Service.class, serviceLink.getServiceId());
                if (service == null || consumedService == null) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                            ServiceConstants.FIELD_SERVICE_ID);
                }
                validateLinkName(serviceLink.getName());
            }
        }
    }
}
