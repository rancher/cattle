package io.cattle.platform.process.snapshot;

import static io.cattle.platform.core.model.tables.SnapshotStoragePoolMapTable.SNAPSHOT_STORAGE_POOL_MAP;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.SnapshotStoragePoolMap;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SnapshotBackup extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    ObjectManager objectManager;
    LockManager lockManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Snapshot snapshot = (Snapshot) state.getResource();

        Volume volume = objectManager.loadResource(Volume.class,
                snapshot.getVolumeId());

        // Use docker storage pool for now, we would introduce other pools later
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

        final long poolId = pool.getId();
        SnapshotStoragePoolMap map = associate(snapshot, poolId);
        create(map, state.getData());

        return new HandlerResult().withShouldContinue(false);
    }

    protected SnapshotStoragePoolMap associate(Snapshot snapshot, long poolId) {
        SnapshotStoragePoolMap map = getMap(snapshot, poolId);
        if (map == null) {
            map = objectManager.create(SnapshotStoragePoolMap.class,
                    SNAPSHOT_STORAGE_POOL_MAP.SNAPSHOT_ID, snapshot.getId(),
                    SNAPSHOT_STORAGE_POOL_MAP.STORAGE_POOL_ID, poolId);
        }

        return map;
    }

    protected SnapshotStoragePoolMap getMap(Snapshot snapshot, long poolId) {
        return mapDao.findNonRemoved(SnapshotStoragePoolMap.class,
                Snapshot.class, snapshot.getId(), StoragePool.class, poolId);
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
}
