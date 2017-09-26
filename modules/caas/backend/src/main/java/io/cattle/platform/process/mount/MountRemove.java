package io.cattle.platform.process.mount;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

public class MountRemove extends AgentBasedProcessHandler {

    public MountRemove(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, serializer, objectManager, processManager, null);
        ignoreReconnecting = true;
        commandName = "storage.volume.remove";
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
