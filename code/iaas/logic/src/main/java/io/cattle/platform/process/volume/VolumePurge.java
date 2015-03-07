package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumePurge extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume) state.getResource();

        deallocate(volume, state.getData());

        for (VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            remove(map, state.getData());
        }

        return new HandlerResult();
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
