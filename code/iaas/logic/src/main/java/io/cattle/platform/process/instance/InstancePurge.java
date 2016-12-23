package io.cattle.platform.process.instance;

import static io.cattle.platform.core.constants.InstanceConstants.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InstancePurge extends AbstractDefaultProcessHandler {

    @Inject
    VolumeDao volumeDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();

        for (Port port : getObjectManager().children(instance, Port.class)) {
            deactivateThenRemove(port, state.getData());
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_INSTANCE_ID)) {
            deactivateThenRemove(link, state.getData());
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_TARGET_INSTANCE_ID)) {
            objectManager.setFields(link, InstanceLinkConstants.FIELD_TARGET_INSTANCE_ID, (Object)null);
        }

        deleteVolumes(instance, state);

        deallocate(instance, null);

        objectManager.reload(instance);

        return null;
    }

    private void deleteVolumes(Instance instance, ProcessState state) {
        Object b = DataAccessor.fieldMap(instance, FIELD_LABELS).get(SystemLabels.LABEL_VOLUME_CLEANUP_STRATEGY);
        String behavior = b != null ? b.toString() : VOLUME_CLEANUP_STRATEGY_UNNAMED;

        Set<? extends Volume> volumes = volumeDao.findNonremovedVolumesWithNoOtherMounts(instance.getId());
        for (Volume v : volumes) {
            String volumeBehavior = migrateVolume(instance, v, behavior);
            if (VOLUME_CLEANUP_STRATEGY_NONE.equals(volumeBehavior)
                    || (!VOLUME_CLEANUP_STRATEGY_UNNAMED.equals(volumeBehavior) && !VOLUME_CLEANUP_STRATEGY_ALL.equals(volumeBehavior))) {
                continue;
            }

            if (VOLUME_CLEANUP_STRATEGY_UNNAMED.equals(volumeBehavior) &&
                    ((StringUtils.length(v.getName()) != 64 || !StringUtils.isAlphanumeric(v.getName()))) && !StringUtils.startsWith(v.getName(), "/")) {
                continue;
            }
            if (CommonStatesConstants.ACTIVE.equals(v.getState()) || CommonStatesConstants.ACTIVATING.equals(v.getState())) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, v,
                        ProcessUtils.chainInData(state.getData(), VolumeConstants.PROCESS_DEACTIVATE, VolumeConstants.PROCESS_REMOVE));
            } else {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, v, state.getData());
            }
        }
    }

    /*
     * Deal with logic where we would set cleanup strategy to none for back populated containers.  Now
     * we do this with a native flag on the volume so we know not to send an event.
     */
    private String migrateVolume(Instance instance, Volume volume, String behavior) {
        if (!VOLUME_CLEANUP_STRATEGY_NONE.equals(behavior) || !instance.getNativeContainer()) {
            return behavior;
        }

        if (volume.getUri() == null || !volume.getUri().startsWith(VolumeConstants.FILE_PREFIX)) {
            return behavior;
        }

        behavior = VOLUME_CLEANUP_STRATEGY_UNNAMED;

        if (!DataAccessor.fieldBool(volume, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            objectManager.setFields(volume, VolumeConstants.FIELD_DOCKER_IS_NATIVE, true);
        }

        return behavior;
    }
}
