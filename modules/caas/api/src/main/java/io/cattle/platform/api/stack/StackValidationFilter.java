package io.cattle.platform.api.stack;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import static io.cattle.platform.core.model.Tables.STACK;

public class StackValidationFilter extends AbstractValidationFilter {

    private static final String STACK_NAME_NOT_UNIQUE = "StackNameNotUnique";
    ObjectManager objectManager;

    public StackValidationFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Stack stack = request.proxyRequestObject(Stack.class);
        long accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
        validateStackName(stack.getName(), accountId);
        return super.create(type, request, next);
    }

    void validateStackName(String stackName, Long accountId) {
        Stack stack = objectManager.findOne(Stack.class, STACK.NAME, stackName, STACK.ACCOUNT_ID, accountId, STACK.REMOVED, null);
        if (stack != null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, STACK_NAME_NOT_UNIQUE);
        }
    }
}
