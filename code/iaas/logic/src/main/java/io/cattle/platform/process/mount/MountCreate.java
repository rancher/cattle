package io.cattle.platform.process.mount;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MountCreate extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;
    @Inject
    LockManager lockManager;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        Mount mount = (Mount)state.getResource();
        Volume volume = objectManager.loadResource(Volume.class, mount.getVolumeId());
        if (!CommonStatesConstants.ACTIVE.equals(volume.getState()) && !CommonStatesConstants.ACTIVATING.equals(volume.getState())) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.ACTIVATE, volume, state.getData());
        }
        return null;
    }
}
