package io.cattle.platform.process.mount;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MountRemove extends AgentBasedProcessHandler {

    @Inject
    GenericMapDao mapDao;

    public MountRemove() {
        super();
        setIgnoreReconnecting(true);
        setCommandName("storage.volume.remove");
        setDataTypeClass(Volume.class);
        setProcessNames("mount.remove");
        setShortCircuitIfAgentRemoved(true);
        setPriority(DEFAULT);
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Instance instance = (Instance)getObjectByRelationship("instance", state.getResource());
        Host host = objectManager.loadResource(Host.class, instance.getHostId());
        return host;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        Mount m = (Mount)state.getResource();
        Volume v = objectManager.loadResource(Volume.class, m.getVolumeId());
        if (DataAccessor.fieldBool(v, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        return v;
    }

    @Override
    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        Mount m = (Mount)state.getResource();
        Volume v = objectManager.loadResource(Volume.class, m.getVolumeId());
        if (DataAccessor.fieldBool(v, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        return v;
    }
}
