package io.cattle.platform.api.stack;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;

public class StackStartAllActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public StackStartAllActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack stack = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class,
                SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null);
        activateServices(services);
        List<? extends Instance> standaloneInstances = objectManager.find(Instance.class, INSTANCE.STACK_ID, stack.getId(),
                INSTANCE.SERVICE_ID, null, INSTANCE.NATIVE_CONTAINER, false, INSTANCE.REMOVED, null);
        startInstances(standaloneInstances);

        return stack;
    }

    private void startInstances(List<? extends Instance> instances) {
        for (Instance instance : instances) {
            if (InstanceConstants.validStatesForStart.contains(instance.getState())) {
                objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_START, instance, null);
            } else if (InstanceConstants.skipStatesForStart.contains(instance.getState())) {
                continue;
            } else {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE,
                        "Instance " + instance.getName() + " is not in valid state to be started: "
                                + instance.getState());
            }
        }
    }

    private void activateServices(List<? extends Service> services) {
        for (Service service : services) {
            if (ServiceConstants.validStatesForActivate.contains(service.getState())) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
            } else if (ServiceConstants.skipStatesForActivate.contains(service.getState())) {
                continue;
            } else {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE,
                        "Service " + service.getName() + " is not in valid state to be activated: "
                                + service.getState());
            }
        }
    }

}