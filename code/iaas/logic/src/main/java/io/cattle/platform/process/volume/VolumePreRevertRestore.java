package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class VolumePreRevertRestore extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { VolumeConstants.PROCESS_REVERT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume v = (Volume)state.getResource();
        state.getData().put("processId", process.getId().toString());
        state.getData().put("volumeName", v.getName());

        Snapshot snapshot = objectManager.loadResource(Snapshot.class, state.getData().get("snapshotId").toString());
        String action = null;
        if ("snapshotted".equals(snapshot.getState()) || "backedup".equals(snapshot.getState())) {
            action = "revert";
        } else {
            action = "restore";
        }
        state.getData().put("action", action);

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}