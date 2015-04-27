package io.cattle.platform.process.snapshot;

import io.cattle.platform.core.constants.SnapshotConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SnapshotCreate extends AgentBasedProcessHandler {

    @Inject
    GenericMapDao mapDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    LockManager lockManager;

    public SnapshotCreate() {
        setShouldContinue(false);
        setCommandName("storage.snapshot.create");
        setProcessNames(new String[] { "snapshot.create" });
        setPriority(Priority.DEFAULT);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HandlerResult result = super.handle(state, process);
        return result.withChainProcessName(SnapshotConstants.PROCESS_BACKUP);
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        final Snapshot snapshot = (Snapshot) dataResource;

        Volume volume = objectManager.loadResource(Volume.class,
                snapshot.getVolumeId());

        StoragePool pool = null;
        for (VolumeStoragePoolMap map : mapDao.findNonRemoved(
                VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            pool = objectManager.loadResource(StoragePool.class,
                    map.getStoragePoolId());
            if ((pool != null) && (pool.getState().equals("active"))) {
                break;
            }
            pool = null;
        }

        if (pool == null) {
            return null;
        }
        return pool;
    }
}
