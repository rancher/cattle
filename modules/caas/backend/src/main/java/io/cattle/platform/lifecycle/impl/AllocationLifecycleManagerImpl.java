package io.cattle.platform.lifecycle.impl;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
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

    public static final DynamicStringProperty EXTERNAL_STYLE = ArchaiusUtil.getString("external.compute.event.target");

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
            if (instance.getHostId() == null) {
                if (!instance.getNativeContainer()) {
                    /* Check if we should defer to external agent for schedule/create/delete.  This should use a different
                       than checking for the agent.
                     */
                    if ("host".equals(EXTERNAL_STYLE.get()) || metadataManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_COMPUTE, instance.getAccountId()).size() > 0) {
                        DataAccessor.setField(instance, InstanceConstants.FIELD_EXTERNAL_COMPUTE_AGENT, true);
                        return;
                    }
                }

                allocatorService.instanceAllocate(instance);
            }

            assignUnmappedVolumes(instance);
            allocatorService.ensureResourcesReservedForStart(instance);
        } catch (FailedToAllocate e) {
            throw new LifecycleException(e.getMessage());
        }
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