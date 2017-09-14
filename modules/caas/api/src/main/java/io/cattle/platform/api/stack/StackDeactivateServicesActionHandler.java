package io.cattle.platform.api.stack;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Arrays;
import java.util.List;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

public class StackDeactivateServicesActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public StackDeactivateServicesActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack env = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, env.getId(),
                SERVICE.REMOVED, null);
        deactivateServices(services);

        return env;
    }

    private void deactivateServices(List<? extends Service> services) {
        List<String> validStates = Arrays.asList(CommonStatesConstants.ACTIVE,
                CommonStatesConstants.ACTIVATING,
                CommonStatesConstants.UPDATING,
                ServiceConstants.STATE_RESTARTING);
        List<String> statesToSkip = Arrays.asList(CommonStatesConstants.REMOVED,
                CommonStatesConstants.REMOVING,
                CommonStatesConstants.INACTIVE,
                CommonStatesConstants.DEACTIVATING);
        for (Service service : services) {
            if (validStates.contains(service.getState())) {
                objectProcessManager.deactivate(service, null);
            } else if (statesToSkip.contains(service.getState())) {
                continue;
            } else {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE,
                        "Service " + service.getName() + " is not in valid state to be deactivated: "
                                + service.getState());
            }
        }
    }
}