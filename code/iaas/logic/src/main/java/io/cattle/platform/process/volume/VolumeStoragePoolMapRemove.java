package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.instance.IgnoreReconnectionAgentHandler;

import javax.inject.Inject;

public class VolumeStoragePoolMapRemove extends IgnoreReconnectionAgentHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Volume volume = (Volume)getObjectByRelationship("volume", state.getResource());
        if (volume.getDeviceNumber() == 0) {
            return null;
        }
        return getObjectByRelationship("storagePool", state.getResource());
    }
}
