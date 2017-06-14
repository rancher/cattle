package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ResourceManagerLocator locator;
    @Inject
    ObjectManager objMgr;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Stack.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Stack env = request.proxyRequestObject(Stack.class);

        if (env.getName().startsWith("-") || env.getName().endsWith("-")) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }

        ResourceManager rm = locator.getResourceManagerByType(type);

        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(ObjectMetaDataManager.NAME_FIELD, env.getName());
        criteria.put(ObjectMetaDataManager.REMOVED_FIELD, new Condition(ConditionType.NULL));
        List<?> existingEnv = rm.list(type, criteria, null);
        if (!existingEnv.isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, "name");
        }
        return super.create(type, request, next);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        Stack stack = objMgr.loadResource(Stack.class, id);
        validateInfraAccess(request, stack, "update");

        return super.update(type, id, request, next);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        Stack stack = objMgr.loadResource(Stack.class, id);
        if (ServiceConstants.DEFAULT_STACK_NAME.equalsIgnoreCase(stack.getName())) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_ACTION,
                    "Default stack can not be removed");
        }

        validateInfraAccess(request, stack, "delete");

        return super.delete(type, id, request, next);
    }

    private void validateInfraAccess(ApiRequest request, Stack stack, String action) {
        if (stack.getSystem() && !ApiUtils.getPolicy().isOption(Policy.MODIFY_INFRA)) {
            String message = String.format("Cannot %s system stack", action);
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "Forbidden", message, null);
        }
    }
}
