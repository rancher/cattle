package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Named;

@Named
public class StackRemove extends AbstractObjectProcessHandler {

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack stack = (Stack) state.getResource();
        removeServices(stack);
        removeDeploymentUnits(stack);
        removeStandaloneContainers(stack);
        removeHosts(stack);
        return null;
    }

    private void removeServices(Stack stack) {
        for (Service service : objectManager.find(Service.class, SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null)) {
            objectProcessManager.remove(service, null);
        }
    }

    private void removeDeploymentUnits(Stack stack) {
        for (DeploymentUnit unit : objectManager.find(DeploymentUnit.class,
                DEPLOYMENT_UNIT.STACK_ID, stack.getId(),
                DEPLOYMENT_UNIT.REMOVED, null)) {
            objectProcessManager.deactivateAndRemove(unit, null);
        }
    }

    private void removeStandaloneContainers(Stack stack) {
        for (Instance instance : objectManager.find(Instance.class, INSTANCE.STACK_ID, stack.getId(),
                INSTANCE.REMOVED, null, INSTANCE.SERVICE_ID, null, INSTANCE.NATIVE_CONTAINER, false)) {
            objectProcessManager.stopAndRemove(instance, null);
        }
    }

    private void removeHosts(Stack stack) {
        for (Host host : objectManager.find(Host.class, HOST.STACK_ID, stack.getId(),
                HOST.REMOVED, null)) {
            objectProcessManager.deactivateAndRemove(host, null);
        }
    }
}
