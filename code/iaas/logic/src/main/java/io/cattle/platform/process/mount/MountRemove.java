package io.cattle.platform.process.mount;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.instance.IgnoreReconnectionAgentHandler;

import java.util.List;

import javax.inject.Inject;

public class MountRemove extends IgnoreReconnectionAgentHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Instance instance = (Instance)getObjectByRelationship("instance", state.getResource());
        List<? extends InstanceHostMap> maps = objectManager.children(instance, InstanceHostMap.class);
        Host host = maps.size() > 0 ? objectManager.loadResource(Host.class, maps.get(0).getHostId()) : null;
        return host;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        Mount m = (Mount)state.getResource();
        Volume v = objectManager.loadResource(Volume.class, m.getVolumeId());
        if (DataAccessor.fieldBool(v, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        List<? extends VolumeStoragePoolMap> maps = objectManager.children(v, VolumeStoragePoolMap.class);
        return maps.size() > 0 ? maps.get(0) : null;
    }

    @Override
    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        Mount m = (Mount)state.getResource();
        Volume v = objectManager.loadResource(Volume.class, m.getVolumeId());
        if (DataAccessor.fieldBool(v, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        List<? extends VolumeStoragePoolMap> maps = objectManager.children(v, VolumeStoragePoolMap.class);
        return maps.size() > 0 ? maps.get(0) : null;
    }
}
