package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

public class StackProcessManager {

    ObjectProcessManager processManager;
    ObjectManager objectManager;

    public StackProcessManager(ObjectProcessManager processManager, ObjectManager objectManager) {
        this.processManager = processManager;
        this.objectManager = objectManager;
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Stack stack = (Stack) state.getResource();
        removeServices(stack);
        removeDeploymentUnits(stack);
        removeStandaloneContainers(stack);
        removeHosts(stack);
        removeVolumeTemplates(stack);
        removeVolumes(stack);
        return null;
    }

    private void removeVolumeTemplates(Stack stack) {
        List<? extends VolumeTemplate> templates = objectManager.find(VolumeTemplate.class,
                VOLUME_TEMPLATE.ACCOUNT_ID, stack.getAccountId(),
                VOLUME_TEMPLATE.REMOVED, null,
                VOLUME_TEMPLATE.STACK_ID, stack.getId());

        for (VolumeTemplate template : templates) {
            processManager.remove(template, null);
        }
    }

    private void removeVolumes(Stack stack) {
        List<? extends Volume> volumes = objectManager.find(Volume.class,
                VOLUME.ACCOUNT_ID, stack.getAccountId(),
                VOLUME.REMOVED, null,
                VOLUME.STACK_ID, stack.getId());
        for (Volume volume : volumes) {
            processManager.executeDeactivateThenRemove(volume, null);
        }
    }

    private void removeServices(Stack stack) {
        for (Service service : objectManager.find(Service.class, SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null)) {
            processManager.remove(service, null);
        }
    }

    private void removeDeploymentUnits(Stack stack) {
        for (DeploymentUnit unit : objectManager.find(DeploymentUnit.class,
                DEPLOYMENT_UNIT.STACK_ID, stack.getId(),
                DEPLOYMENT_UNIT.REMOVED, null)) {
            processManager.deactivateThenRemove(unit, null);
        }
    }

    private void removeStandaloneContainers(Stack stack) {
        for (Instance instance : objectManager.find(Instance.class, INSTANCE.STACK_ID, stack.getId(),
                INSTANCE.REMOVED, null, INSTANCE.SERVICE_ID, null, INSTANCE.NATIVE_CONTAINER, false)) {
            processManager.stopThenRemove(instance, null);
        }
    }

    private void removeHosts(Stack stack) {
        for (Host host : objectManager.find(Host.class, HOST.STACK_ID, stack.getId(),
                HOST.REMOVED, null)) {
            processManager.deactivateThenRemove(host, null);
        }
    }

}
