package io.cattle.platform.process.mount;

import static io.cattle.platform.core.model.tables.MountTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.lock.MountVolumeLock;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MountProcessManager {

    private static final List<String> MOUNT_STATES = Arrays.asList(
            CommonStatesConstants.INACTIVE,
            CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED,
            CommonStatesConstants.REMOVING);

    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public MountProcessManager(ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Mount mount = (Mount)state.getResource();
        Volume volume = objectManager.loadResource(Volume.class, mount.getVolumeId());
        /* This is an upgrade issue with old data from <= 1.1.x */
        if (volume.getRemoved() != null) {
            return null;
        }
        if (!CommonStatesConstants.ACTIVE.equals(volume.getState()) && !CommonStatesConstants.ACTIVATING.equals(volume.getState())
                && !CommonStatesConstants.RESTORING.equals(volume.getState())) {
            if (CommonStatesConstants.REQUESTED.equals(volume.getState()) || CommonStatesConstants.REGISTERING.equals(volume.getState())) {
                processManager.scheduleStandardProcess(StandardProcess.CREATE, volume,
                        ProcessUtils.chainInData(state.getData(), VolumeConstants.PROCESS_CREATE, VolumeConstants.PROCESS_ACTIVATE));
            } else {
                processManager.scheduleStandardProcess(StandardProcess.ACTIVATE, volume, state.getData());
            }
        }
        return null;
    }

    public HandlerResult deactivate(final ProcessState state, ProcessInstance process) {
        Mount mount = (Mount)state.getResource();
        lockManager.lock(new MountVolumeLock(mount.getVolumeId()), () -> {
            Map<Object, Object> criteria = new HashMap<>();
            criteria.put(MOUNT.ID, new Condition(ConditionType.NE, mount.getId()));
            criteria.put(MOUNT.STATE, new Condition(ConditionType.NOTIN, MOUNT_STATES));
            criteria.put(MOUNT.VOLUME_ID, mount.getVolumeId());
            Mount mount2 = objectManager.findAny(Mount.class, criteria);
            if (mount2 == null) {
                Volume vol = objectManager.loadResource(Volume.class, mount.getVolumeId());
                if (CommonStatesConstants.ACTIVE.equals(vol.getState()) || CommonStatesConstants.ACTIVATING.equals(vol.getState())) {
                    processManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, vol, null);
                }
            }
            return null;
        });
        return null;
    }


}
