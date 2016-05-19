package io.cattle.platform.process.volume;

import static io.cattle.platform.core.model.tables.SnapshotTable.*;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

@Named
public class VolumePostRevertRestore extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { VolumeConstants.PROCESS_REVERT, VolumeConstants.PROCESS_RESTORE_FROM_BACKUP };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        List<Snapshot> snapshots = null;
        if (VolumeConstants.PROCESS_RESTORE_FROM_BACKUP.equalsIgnoreCase(process.getName())) {
            snapshots = objectManager.children(volume, Snapshot.class);
        } else if (VolumeConstants.PROCESS_REVERT.equalsIgnoreCase(process.getName())) {
            Snapshot snapshot = objectManager.loadResource(Snapshot.class, state.getData().get("snapshotId").toString());
            Map<Object, Object> criteria = new HashMap<Object, Object>();
            criteria.put(SNAPSHOT.VOLUME_ID, volume.getId());
            criteria.put(SNAPSHOT.REMOVED, null);
            criteria.put(SNAPSHOT.ID, new Condition(ConditionType.GT, snapshot.getId()));
            snapshots = objectManager.find(Snapshot.class, criteria);
        } else {
            throw new IllegalStateException("Unknown process: " + process.getName());
        }

        for (Snapshot s : snapshots) {
            if (s.getRemoved() == null) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, s, null);
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}