package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.List;

import javax.inject.Named;

@Named
public class StackRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "stack.remove" };
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
            if (!(volume.getState().equals(CommonStatesConstants.REMOVED) || volume.getState().equals(
                    CommonStatesConstants.REMOVING))) {
                try {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, volume,
                            null);
                } catch (ProcessCancelException e) {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE,
                            volume, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                            ExternalEventConstants.PROC_VOL_DEACTIVATE,
                                            ExternalEventConstants.PROC_VOL_REMOVE));
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
