package io.cattle.platform.process.volume;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeDeallocate extends AbstractDefaultProcessHandler {

    @Inject
    GenericMapDao mapDao;
    @Inject
    AllocatorService allocatorService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Map<String, Object> result = new HashMap<String, Object>();
        Volume volume = (Volume) state.getResource();

        allocatorService.volumeDeallocate(volume);

        for (VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            deactivateThenScheduleRemove(map, state.getData());
        }

        return new HandlerResult(result);
    }
}
