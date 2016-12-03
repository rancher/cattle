package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeAllocate extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Volume volume = (Volume) state.getResource();

        for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            CollectionUtils.addToMap(allocationData, "volume:" + volume.getId(), map.getVolumeId(), HashSet.class);
            create(map, state.getData());
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
