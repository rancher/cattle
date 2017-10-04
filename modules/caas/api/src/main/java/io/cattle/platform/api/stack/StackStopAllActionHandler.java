package io.cattle.platform.api.stack;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.List;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

public class StackStopAllActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public StackStopAllActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager) {
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack stack = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null);
        deactivateServices(services);

        List<? extends Instance> standaloneInstances = objectManager.find(Instance.class, INSTANCE.STACK_ID, stack.getId(),
                INSTANCE.SERVICE_ID, null, INSTANCE.NATIVE_CONTAINER, false, INSTANCE.REMOVED, null);
        stopInstances(standaloneInstances);

        return stack;
    }

    private void stopInstances(List<? extends Instance> instances) {
        for (Instance instance : instances) {
            if (InstanceConstants.validStatesForStop.contains(instance.getState())) {
                objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance, null);
            } else if (InstanceConstants.skipStatesForStop.contains(instance.getState())) {
                continue;
            } else {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE,
                        "Instance " + instance.getName() + " is not in valid state to be stopped: "
                                + instance.getState());
            }
        }
    }

    private void deactivateServices(List<? extends Service> services) {
        for (Service service : services) {
            if (ServiceConstants.skipStatesForDeactivate.contains(service.getState())) {
                continue;
            }
            objectProcessManager.deactivate(service, null);
        }
    }
}

