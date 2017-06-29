package io.cattle.platform.process.mount;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class MountRemove extends AgentBasedProcessHandler {

    GenericMapDao mapDao;

    public MountRemove(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        ignoreReconnecting = true;
        commandName = "storage.volume.remove";
        dataTypeClass = Volume.class;
        shortCircuitIfAgentRemoved = true;
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        return objectManager.loadResource(Instance.class, ((Mount) state.getResource()).getInstanceId());
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
