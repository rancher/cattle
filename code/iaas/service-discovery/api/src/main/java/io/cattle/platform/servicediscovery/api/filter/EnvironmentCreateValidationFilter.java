package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

public class EnvironmentCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Environment.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Environment env = request.proxyRequestObject(Environment.class);

        if (env.getName().startsWith("-") || env.getName().endsWith("-")) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }

        Environment existingEnv = objectManager.findOne(Environment.class, ENVIRONMENT.NAME, env.getName(),
                ENVIRONMENT.REMOVED, null);
        if (existingEnv != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "name");
        }
        return super.create(type, request, next);
    }
}
