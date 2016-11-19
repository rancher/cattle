package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.EventBasedProcessHandler;

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
        Volume volume = (Volume) state.getResource();

        for (VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            deactivateThenScheduleRemove(map, state.getData());
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
