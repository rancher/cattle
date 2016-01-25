package io.cattle.platform.process.mount;

import static io.cattle.platform.core.model.tables.MountTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.lock.MountVolumeLock;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MountDeactivate extends AbstractDefaultProcessHandler {

    public static final List<Object> MOUNT_STATES = Arrays.asList(new Object[] { CommonStatesConstants.INACTIVE, CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING });
    @Inject
    LockManager lockManager;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Mount mount = (Mount)state.getResource();
        lockManager.lock(new MountVolumeLock(mount.getVolumeId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Map<Object, Object> criteria = new HashMap<Object, Object>();
                criteria.put(MOUNT.ID, new Condition(ConditionType.NE, mount.getId()));
                criteria.put(MOUNT.STATE, new Condition(ConditionType.NOTIN, MOUNT_STATES));
                criteria.put(MOUNT.VOLUME_ID, mount.getVolumeId());
                Mount mount2 = objectManager.findAny(Mount.class, criteria);
                if (mount2 == null) {
                    Volume vol = objectManager.loadResource(Volume.class, mount.getVolumeId());
                    if (CommonStatesConstants.ACTIVE.equals(vol.getState()) || CommonStatesConstants.ACTIVATING.equals(vol.getState())) {
                        objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, vol, null);
                    }
                }
            }
        });
        return null;
    }
}
