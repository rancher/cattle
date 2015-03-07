package io.cattle.platform.process.instance;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.EventBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceAllocate extends EventBasedProcessHandler {

    GenericMapDao mapDao;

    public InstanceAllocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Map<String, Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Instance instance = (Instance) state.getResource();

        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            CollectionUtils.addToMap(allocationData, "instance:" + instance.getId(), map.getHostId(), HashSet.class);
            create(map, state.getData());
        }

        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for (Volume v : volumes) {
            allocate(v, state.getData());
        }

        volumes = getObjectManager().children(instance, Volume.class);
        for (Volume v : volumes) {
            for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, v.getId())) {
                CollectionUtils.addToMap(allocationData, "volume:" + v.getId(), map.getVolumeId(), HashSet.class);
                create(map, state.getData());
            }
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
