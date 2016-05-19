package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Backup;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

import javax.inject.Inject;

public class VolumeRevertRestoreAgentProcessHandler extends AgentBasedProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Volume instance = (Volume)state.getResource();
        List<? extends VolumeStoragePoolMap> maps = objectManager.children(instance, VolumeStoragePoolMap.class);
        StoragePool host = maps.size() > 0 ? objectManager.loadResource(StoragePool.class, maps.get(0).getStoragePoolId()) : null;
        return host;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        if (process.getName().equalsIgnoreCase(VolumeConstants.PROCESS_REVERT)) {
            Snapshot snapshot = objectManager.loadResource(Snapshot.class, state.getData().get("snapshotId").toString());
            return snapshot;
        } else if (process.getName().equalsIgnoreCase(VolumeConstants.PROCESS_RESTORE_FROM_BACKUP)) {
            Backup backup = objectManager.loadResource(Backup .class, state.getData().get("backupId").toString());
            return backup;
        }
        throw new IllegalStateException("Unsupported process: " + process.getName());
    }
}
