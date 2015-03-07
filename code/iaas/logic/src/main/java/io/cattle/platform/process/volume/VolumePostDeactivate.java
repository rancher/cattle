package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class VolumePostDeactivate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "volume.deactivate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume) state.getResource();

        if (Boolean.TRUE.equals(state.getData().get(VolumeConstants.REMOVE_OPTION))) {
            String chainProcess = objectProcessManager.getStandardProcessName(StandardProcess.REMOVE, volume);
            return new HandlerResult().withChainProcessName(chainProcess);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}