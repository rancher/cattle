package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

import javax.inject.Inject;

public class SnapshotAgentProcessHandler extends AgentBasedProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Object resource = state.getResource();
        Volume instance = (Volume)getObjectByRelationship("volume", resource);
        List<? extends VolumeStoragePoolMap> maps = objectManager.children(instance, VolumeStoragePoolMap.class);
        StoragePool sp = maps.size() > 0 ? objectManager.loadResource(StoragePool.class, maps.get(0).getStoragePoolId()) : null;
        return sp;
    }
}
