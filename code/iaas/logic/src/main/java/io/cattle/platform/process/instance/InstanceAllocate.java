package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
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
    @Inject
    EventService eventService;

    @Inject
    NicDao nicDao;

    @Inject
    GenericMapDao mapDao;

    public InstanceAllocate() {
        setPriority(DEFAULT);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId()).size() > 0) {
            return postEvent(state, process, new HashMap<Object, Object>());
        }
        return super.handle(state, process);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Map<String, Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Instance instance = (Instance) state.getResource();
        Long hostId = null;

        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            CollectionUtils.addToMap(allocationData, "instance:" + instance.getId(), map.getHostId(), HashSet.class);
            create(map, state.getData());
            hostId = map.getHostId();
        }

        result.put(InstanceConstants.FIELD_HOST_ID, hostId);

        List<Volume> volumes = getObjectManager().children(instance, Volume.class);
        List<Volume> dataMountVolumes = InstanceHelpers.extractVolumesFromMounts(instance, getObjectManager());
        volumes.addAll(dataMountVolumes);

        for (Volume v : volumes) {
            allocate(v, state.getData());
        }

        volumes = getObjectManager().children(instance, Volume.class);
        volumes.addAll(dataMountVolumes);

        for (Volume v : volumes) {
            for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, v.getId())) {
                CollectionUtils.addToMap(allocationData, "volume:" + v.getId(), map.getVolumeId(), HashSet.class);
                create(map, state.getData());
            }
        }

        return new HandlerResult(result);
    }

}
