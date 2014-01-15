package io.github.ibuildthecloud.dstack.process.volume;

import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

@Named
public class VolumeDeactivate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        Set<Long> pools = new HashSet<Long>();
        for ( VolumeStoragePoolMap map : getObjectManager().children(volume, VolumeStoragePoolMap.class) ) {
            if ( map.getRemoved() == null ) {
                deactivate(map, state.getData());
                pools.add(map.getStoragePoolId());
            }
        }

        return new HandlerResult("_deactivatedPools", pools);
    }

}