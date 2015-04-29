package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);
        
        Service existingService = objectManager.findOne(Service.class, SERVICE.NAME, service.getName(),
                SERVICE.ENVIRONMENT_ID, service.getEnvironmentId(), SERVICE.REMOVED, null);
        if (existingService != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "name");
        }

        if (service.getName().startsWith("-") || service.getName().endsWith("-")) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }

        return super.create(type, request, next);
    }
}
