package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.lifecycle.AllocationLifecycleManager;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class AllocationLifecycleManagerImpl implements AllocationLifecycleManager {

    AllocatorService allocatorService;
    VolumeDao volumeDao;
    ObjectManager objectManager;

    public AllocationLifecycleManagerImpl(AllocatorService allocatorService, VolumeDao volumeDao, ObjectManager objectManager) {
        super();
        this.allocatorService = allocatorService;
        this.volumeDao = volumeDao;
        this.objectManager = objectManager;
    }

    @Override
    public void preStart(Instance instance) {
        if (instance.getHostId() == null) {
            allocatorService.instanceAllocate(instance);
        }

        assignUnmappedVolumes(instance);
        allocatorService.ensureResourcesReservedForStart(instance);
    }

    protected void assignUnmappedVolumes(Instance instance) {
        List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);

        for (Volume v : volumes) {
            if (v.getStoragePoolId() == null) {
                Long storagePoolId = volumeDao.findPoolForVolumeAndHost(v, instance.getHostId());
                if (storagePoolId != null) {
                    objectManager.setFields(v, VOLUME.STORAGE_POOL_ID, storagePoolId);
                }
            }
        }
    }

    @Override
    public void postStop(Instance instance) {
        allocatorService.ensureResourcesReleasedForStop(instance);
    }

    @Override
    public void postRemove(Instance instance) {
        allocatorService.instanceDeallocate(instance);
    }

}
