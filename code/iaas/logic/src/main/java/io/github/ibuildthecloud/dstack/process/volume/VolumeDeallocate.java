package io.github.ibuildthecloud.dstack.process.volume;

import io.github.ibuildthecloud.dstack.core.dao.GenericMapDao;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.common.handler.EventBasedProcessHandler;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeDeallocate extends EventBasedProcessHandler {

    GenericMapDao mapDao;

    public VolumeDeallocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Volume volume = (Volume)state.getResource();

        for ( VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId()) ) {
            remove(map, state.getData());
        }

        return new HandlerResult(result);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
