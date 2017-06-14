package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceAllocate extends AbstractDefaultProcessHandler {

    @Inject
    AllocatorService allocatorService;
    @Inject
    GenericMapDao mapDao;
    @Inject
    VolumeDao volumeDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId()).size() == 0) {
            allocatorService.instanceAllocate(instance);
        }
        return afterAllocate(state, process, new HashMap<>());
    }

    protected HandlerResult afterAllocate(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Instance instance = (Instance) state.getResource();
        Long hostId = null;

        for (InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId())) {
            createIfNot(map, state.getData());
            hostId = map.getHostId();
        }

        List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, getObjectManager());

        for (Volume v : volumes) {
            List<? extends VolumeStoragePoolMap> maps = mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, v.getId());
            if (maps.size() == 0) {
                Long storagePoolId = volumeDao.findPoolForVolumeAndHost(v, hostId);
                if (storagePoolId != null) {
                    /* Don't see any point is orchestrating this so immediately putting to active.
                     * If some reason is found later to orchestrate, then there is not harm.
                     */
                    objectManager.create(VolumeStoragePoolMap.class,
                        VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID, storagePoolId,
                        VOLUME_STORAGE_POOL_MAP.VOLUME_ID, v.getId(),
                        VOLUME_STORAGE_POOL_MAP.STATE, CommonStatesConstants.ACTIVE);


                }
            } else {
                for (VolumeStoragePoolMap map : maps) {
                    createIfNot(map, null);
                }
            }
        }

        return new HandlerResult(InstanceConstants.FIELD_HOST_ID, hostId);
    }
}