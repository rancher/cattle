package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.AllocationLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class AllocationLifecycleManagerImpl implements AllocationLifecycleManager {

    AllocatorService allocatorService;
    VolumeDao volumeDao;
    ObjectManager objectManager;
    MetadataManager metadataManager;

    public AllocationLifecycleManagerImpl(AllocatorService allocatorService, VolumeDao volumeDao, ObjectManager objectManager, MetadataManager metadataManager) {
        this.allocatorService = allocatorService;
        this.volumeDao = volumeDao;
        this.objectManager = objectManager;
        this.metadataManager = metadataManager;
    }

    @Override
    public void preStart(Instance instance) throws LifecycleException {
        try {
            if (shouldAllocateAndSetOrchestration(instance)) {
                allocatorService.instanceAllocate(instance);
            }

            assignUnmappedVolumes(instance);
            allocatorService.ensureResourcesReservedForStart(instance);
        } catch (FailedToAllocate e) {
            throw new LifecycleException(e.getMessage());
        }
    }

    protected boolean shouldAllocateAndSetOrchestration(Instance instance) {
        if (instance.getHostId() != null) {
            return false;
        }

        if (DataAccessor.fieldLong(instance, InstanceConstants.FIELD_REQUESTED_HOST_ID) != null) {
            return true;
        }

        if (ClusterConstants.ORCH_CATTLE.equals(DataAccessor.getLabel(instance, SystemLabels.LABEL_ORCHESTRATION))) {
            return true;
        }

        return false;
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
