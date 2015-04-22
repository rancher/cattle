package io.cattle.platform.process.snapshot;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.SnapshotStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SnapshotRemove extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    LockManager lockManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Snapshot snapshot = (Snapshot) state.getResource();

        for (SnapshotStoragePoolMap map : mapDao.findToRemove(
                SnapshotStoragePoolMap.class, Snapshot.class, snapshot.getId())) {
            remove(map, state.getData());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        return new HandlerResult(result);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
