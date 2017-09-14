package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.allocator.constraint.HostAffinityConstraint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.VolumeLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.lock.InstanceVolumeAccessModeLock;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.object.util.DataAccessor.*;
import static io.github.ibuildthecloud.gdapi.condition.Condition.*;

public class VolumeLifecycleManagerImpl implements VolumeLifecycleManager {

    private static final String LABEL_VOLUME_AFFINITY = "io.rancher.scheduler.affinity:volumes";

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    StoragePoolDao storagePoolDao;
    VolumeDao volumeDao;
    LockManager lockManager;

    public VolumeLifecycleManagerImpl(ObjectManager objectManager, ObjectProcessManager processManager, StoragePoolDao storagePoolDao, VolumeDao volumeDao,
            LockManager lockManager) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.storagePoolDao = storagePoolDao;
        this.volumeDao = volumeDao;
        this.lockManager = lockManager;
    }

    @Override
    public void create(Instance instance) throws LifecycleException {
        String va = getLabel(instance, LABEL_VOLUME_AFFINITY);
        Set<String> affinities = new HashSet<>();
        if (StringUtils.isNotBlank(va)) {
            affinities.addAll(Arrays.asList(va.split(",")));
        }

        String volumeDriver = fieldString(instance, InstanceConstants.FIELD_VOLUME_DRIVER);

        List<String> dataVolumes = fieldStringList(instance, InstanceConstants.FIELD_DATA_VOLUMES);
        Map<String, Object> dataVolumeMounts = fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        List<String> newDataVolumes = new ArrayList<>();
        for (String v : dataVolumes) {
            String volName;
            String volPath;
            String[] parts = v.split(":", 2);
             if (parts.length == 2 && !parts[0].startsWith("/") && parts[1].startsWith("/")) {
                // named volume
                 volName = parts[0];
                 volPath = parts[1];
            } else if (isNonlocalDriver(volumeDriver) && parts.length == 2 && parts[0].startsWith("/") && !parts[1].startsWith("/")) {
                // anonymous volume with mount permissions
                volName = UUID.randomUUID().toString();
                volPath = parts[0] + ":" + parts[1];
                v = volName + ":" + volPath;
            } else if (isNonlocalDriver(volumeDriver) && parts.length == 1 && parts[0].startsWith("/")) {
                // anonymous volume
                volName = UUID.randomUUID().toString();
                volPath = parts[0];
                v = volName + ":" + volPath;
            } else {
                newDataVolumes.add(v);
                continue;
            }

            boolean createVol = true;
            if (volName != null) {
                List<? extends Volume> volumes = volumeDao.findSharedOrUnmappedVolumes(instance.getAccountId(), volName);
                if (volumes.isEmpty() && affinities.contains(volName)) {
                    // Any volumes found here will be mapped to local (docker, sim) pools
                    volumes = objectManager.find(Volume.class, VOLUME.ACCOUNT_ID, instance.getAccountId(), VOLUME.NAME, volName, VOLUME.REMOVED, null);
                }

                if (volumes.size() == 1) {
                    dataVolumeMounts.put(volPath, volumes.get(0).getId());
                    createVol = false;
                } else if (volumes.size() > 1) {
                    throw new LifecycleException(String.format("Could not process named volume %s. More than one volume " + "with that name exists.", volName));
                }
            }

            if (createVol && isNonlocalDriver(volumeDriver)) {
                Volume newVol = volumeDao.createVolumeForDriver(instance.getClusterId(), instance.getAccountId(), volName, volumeDriver);
                dataVolumeMounts.put(volPath, newVol.getId());
            }
            newDataVolumes.add(v);
        }

        setField(instance, InstanceConstants.FIELD_DATA_VOLUMES, newDataVolumes);
        setField(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumeMounts);
    }

    @Override
    public void preStart(Instance instance) {
        List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);
        for (final Volume v : volumes) {
            setVolumeAccessMode(v);
            setupEc2AzLabels(instance, v);
        }
    }

    @Override
    public void preRemove(Instance instance) {
        deactivateMounts(instance);
        deleteUnmanagedAndNativeVolume(instance);
    }

    private void deleteUnmanagedAndNativeVolume(Instance instance) {
        Object b = fieldMap(instance, FIELD_LABELS).get(SystemLabels.LABEL_VOLUME_CLEANUP_STRATEGY);
        String behavior = b != null ? b.toString() : VOLUME_CLEANUP_STRATEGY_UNNAMED;

        Set<? extends Volume> volumes = volumeDao.findNonremovedVolumesWithNoOtherMounts(instance.getId());
        for (Volume v : volumes) {
            String volumeBehavior = migrateVolume(instance, v, behavior);
            if (VOLUME_CLEANUP_STRATEGY_NONE.equals(volumeBehavior)
                    || (!VOLUME_CLEANUP_STRATEGY_UNNAMED.equals(volumeBehavior) && !VOLUME_CLEANUP_STRATEGY_ALL.equals(volumeBehavior))) {
                continue;
            }

            if (VOLUME_CLEANUP_STRATEGY_UNNAMED.equals(volumeBehavior) &&
                    ((StringUtils.length(v.getName()) != 64 || !StringUtils.isAlphanumeric(v.getName()))) && !StringUtils.startsWith(v.getName(), "/")) {
                continue;
            }
            if (CommonStatesConstants.ACTIVE.equals(v.getState()) || CommonStatesConstants.ACTIVATING.equals(v.getState())) {
                processManager.deactivateThenRemove(v, null);
            } else {
                processManager.remove(v, null);
            }
        }
    }

    /*
     * Deal with logic where we would set cleanup strategy to none for back populated containers.  Now
     * we do this with a native flag on the volume so we know not to send an event.
     */
    private String migrateVolume(Instance instance, Volume volume, String behavior) {
        if (!VOLUME_CLEANUP_STRATEGY_NONE.equals(behavior) || !instance.getNativeContainer()) {
            return behavior;
        }

        if (volume.getUri() == null || !volume.getUri().startsWith(VolumeConstants.FILE_PREFIX)) {
            return behavior;
        }

        behavior = VOLUME_CLEANUP_STRATEGY_UNNAMED;

        if (!DataAccessor.fieldBool(volume, VolumeConstants.FIELD_DOCKER_IS_NATIVE)) {
            objectManager.setFields(volume, VolumeConstants.FIELD_DOCKER_IS_NATIVE, true);
        }

        return behavior;
    }


    protected void deactivateMounts(Instance instance) {
        List<Mount> mounts = objectManager.find(Mount.class,
                MOUNT.REMOVED, isNull(),
                MOUNT.STATE, notIn(VolumeLifecycleManager.MOUNT_STATES),
                MOUNT.INSTANCE_ID, instance.getId());

        for (Mount mount : mounts) {
            processManager.deactivate(mount, null);
        }
    }

    protected void setVolumeAccessMode(Volume v) {
        String driver = fieldString(v, VolumeConstants.FIELD_VOLUME_DRIVER);
        if (StringUtils.isNotEmpty(driver) && !VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(v.getClusterId(), driver);
            if (pools.size() == 0) {
                return;
            }
            StoragePool sp = pools.get(0);

            final String accessMode = sp.getVolumeAccessMode();
            if (StringUtils.isNotEmpty(accessMode) && StringUtils.isEmpty(v.getAccessMode()) && !accessMode.equals(v.getAccessMode())) {
                lockManager.lock(new InstanceVolumeAccessModeLock(v.getId()), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        objectManager.setFields(v, VOLUME.ACCESS_MODE, accessMode);
                    }
                });
            }
        }
    }

    protected void setupEc2AzLabels(Instance instance, Volume v) {
        Map<String, Object> driver_opts = DataAccessor.fieldMap(v, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS);

        if (driver_opts.containsKey(VolumeConstants.EC2_AZ)) {
            String zone = ObjectUtils.toString(driver_opts.get(VolumeConstants.EC2_AZ));
            String label = String.format("%s=%s", VolumeConstants.HOST_ZONE_LABEL_KEY, zone);
            Object originalLabel = getLabel(instance, HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL);
            if (originalLabel != null && !StringUtils.isEmpty(originalLabel.toString())) {
                label = originalLabel.toString() + "," + label;
            }
            setLabel(instance, HostAffinityConstraint.LABEL_HEADER_AFFINITY_HOST_LABEL, label);
        }
    }


    boolean isNonlocalDriver(String volumeDriver) {
        return StringUtils.isNotEmpty(volumeDriver) && !VolumeConstants.LOCAL_DRIVER.equals(volumeDriver);
    }


}
