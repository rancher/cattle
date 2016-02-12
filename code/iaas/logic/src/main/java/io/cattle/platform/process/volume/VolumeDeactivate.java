package io.cattle.platform.process.volume;

import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

@Named
public class VolumeDeactivate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume) state.getResource();

        Set<Long> pools = new HashSet<Long>();
        for (VolumeStoragePoolMap map : getObjectManager().children(volume, VolumeStoragePoolMap.class)) {
            if (map.getRemoved() == null) {
                try {
                    deactivate(map, state.getData());
                } catch (ProcessCancelException e) {
                    remove(map, state.getData());
                }
                pools.add(map.getStoragePoolId());
            }
        }

        return new HandlerResult("_deactivatedPools", pools);
    }

}