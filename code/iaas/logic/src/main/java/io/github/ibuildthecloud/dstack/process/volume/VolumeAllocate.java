package io.github.ibuildthecloud.dstack.process.volume;

import io.github.ibuildthecloud.dstack.core.dao.VolumeDao;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.common.handler.EventBasedProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeAllocate extends EventBasedProcessHandler {

    VolumeDao volumeDao;

    public VolumeAllocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Map<String,Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Volume volume = (Volume)state.getResource();

        for ( VolumeStoragePoolMap map : volumeDao.findNonRemovedVolumeStoragePoolMaps(volume.getId()) ) {
            CollectionUtils.addToMap(allocationData, "volume:" + volume.getId(), map.getVolumeId(), HashSet.class);
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, state.getData());
        }

        return new HandlerResult(result);
    }

    public VolumeDao getVolumeDao() {
        return volumeDao;
    }

    @Inject
    public void setVolumeDao(VolumeDao volumeDao) {
        this.volumeDao = volumeDao;
    }
}
