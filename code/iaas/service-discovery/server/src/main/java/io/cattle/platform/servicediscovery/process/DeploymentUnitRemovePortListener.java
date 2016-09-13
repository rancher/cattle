package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Volume;
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

public class DeploymentUnitRemovePortListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "deploymentunit.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DeploymentUnit du = (DeploymentUnit) state.getResource();

        removeVolumes(du);

        return null;
    }

    private void removeVolumes(DeploymentUnit du) {
        List<? extends Volume> volumes = objectManager.find(Volume.class, VOLUME.REMOVED, null,
                VOLUME.DEPLOYMENT_UNIT_ID, du.getId());
        for (Volume volume : volumes) {
            if (!(volume.getState().equals(CommonStatesConstants.REMOVED) || volume.getState().equals(
                    CommonStatesConstants.REMOVING))) {
                try {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, volume,
                            null);
                } catch (ProcessCancelException e) {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE,
                            volume, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                    StandardProcess.DEACTIVATE.name(), InstanceConstants.PROCESS_REMOVE));
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
