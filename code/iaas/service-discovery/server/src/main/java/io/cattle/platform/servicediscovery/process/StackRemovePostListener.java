package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.VolumeConstants.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack stack = (Stack) state.getResource();
        removeVolumeTemplates(stack);
        removeVolumes(stack);
        return null;
    }

    private void removeVolumeTemplates(Stack stack) {
        List<? extends VolumeTemplate> templates = objectManager.find(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                stack.getAccountId(), VOLUME_TEMPLATE.REMOVED, null,
                VOLUME_TEMPLATE.STACK_ID, stack.getId());
        for (VolumeTemplate template : templates) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, template, null);
        }
    }

    private void removeVolumes(Stack stack) {
        List<? extends Volume> volumes = objectManager.find(Volume.class, VOLUME.ACCOUNT_ID,
                stack.getAccountId(), VOLUME.REMOVED, null,
                VOLUME.STACK_ID, stack.getId());
        for (Volume volume : volumes) {
            String state = volume.getState();
            if (!REMOVED.equals(state) && !REMOVING.equals(state)){
                try {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, volume, null);
                } catch (ProcessCancelException e) {
                    volume = resourceMonitor.waitForState(volume, STATE_DETACHED, 3000L);
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, volume, null);
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
