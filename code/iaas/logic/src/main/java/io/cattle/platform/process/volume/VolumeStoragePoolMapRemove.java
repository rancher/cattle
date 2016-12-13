package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;

import com.github.dockerjava.api.model.Volume;

public class VolumeStoragePoolMapRemove extends io.cattle.platform.process.common.handler.AgentBasedProcessHandler {

    @Override
    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        Object obj = super.getEventResource(state, process);
        if (obj instanceof Volume && DataAccessor.fieldBool(obj, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        return obj;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        Object obj = super.getDataResource(state, process);
        if (obj instanceof Volume && DataAccessor.fieldBool(obj, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            return null;
        }
        return obj;
    }

}
