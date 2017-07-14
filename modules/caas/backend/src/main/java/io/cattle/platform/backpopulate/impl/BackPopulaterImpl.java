package io.cattle.platform.backpopulate.impl;

import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.docker.process.lock.DockerStoragePoolVolumeCreateLock;
import io.cattle.platform.docker.transform.DockerInspectTransformVolume;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.lock.MountVolumeLock;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.object.util.DataAccessor.*;

public class BackPopulaterImpl implements BackPopulater {

    private static final Logger log = LoggerFactory.getLogger(BackPopulaterImpl.class);

    JsonMapper jsonMapper;
    VolumeDao volumeDao;
    StoragePoolDao storagePoolDao;
    LockManager lockManager;
    DockerTransformer transformer;
    InstanceDao instanceDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public BackPopulaterImpl(JsonMapper jsonMapper, VolumeDao volumeDao, LockManager lockManager, DockerTransformer transformer, InstanceDao instanceDao,
            ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.jsonMapper = jsonMapper;
        this.volumeDao = volumeDao;
        this.lockManager = lockManager;
        this.transformer = transformer;
        this.instanceDao = instanceDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public void update(Instance instance) {
        Host host = objectManager.loadResource(Host.class, instance.getHostId());

        processVolumes(instance, host);

        processLabels(instance);

        processRemainingFields(instance);
    }

    @SuppressWarnings("unchecked")
    void processLabels(Instance instance) {
        Map<String, Object> inspect = DataAccessor.fieldMapRO(instance, FIELD_DOCKER_INSPECT);
        if (inspect == null) {
            return;
        }
        transformer.setLabels(instance, inspect);
    }

    @SuppressWarnings({ "unchecked" })
    protected void processRemainingFields(Instance instance) {
        Map<String, Object> inspect = DataAccessor.fieldMapRO(instance, FIELD_DOCKER_INSPECT);
        if (inspect == null || instance.getNativeContainer() == null || !instance.getNativeContainer().booleanValue()) {
            return;
        }
        transformer.transform(inspect, instance);
    }

    private Map<String, StoragePool> getPoolsByName(long hostId) {
        Map<String, StoragePool> pools = new HashMap<>();
        for (StoragePool pool : storagePoolDao.findNonRemovedStoragePoolByHost(hostId)) {
            if (DockerStoragePoolConstants.DOCKER_KIND.equals(pool.getKind()) &&
                    (VolumeConstants.LOCAL_DRIVER.equals(pool.getDriverName()) || StringUtils.isEmpty(pool.getDriverName()))) {
                pools.put(VolumeConstants.LOCAL_DRIVER, pool);
            }
            if (StringUtils.isNotEmpty(pool.getDriverName())) {
                pools.put(pool.getDriverName(), pool);
            }
        }
        return pools;
    }

    @SuppressWarnings({ "unchecked" })
    protected void processVolumes(Instance instance, Host host) {
        Map<String, Object> inspect = DataAccessor.fieldMapRO(instance, FIELD_DOCKER_INSPECT);
        if (inspect == null) {
            return;
        }
        List<Object> mounts = (List<Object>) inspect.get("Mounts");
        if (mounts == null) {
            return;
        }
        List<DockerInspectTransformVolume> dockerVolumes = transformer.transformVolumes(inspect, mounts);

        if (dockerVolumes.size() == 0) {
            /* If there are no volumes avoid looking for a pool because one may not exists
             * for this host and we don't want to warn about that
             */
            return;
        }

        Map<String, StoragePool> pools = getPoolsByName(instance.getHostId());
        for (DockerInspectTransformVolume dVol : dockerVolumes) {
            StoragePool pool = pools.get(dVol.getDriver());
            if (pool == null) {
                continue;
            }

            Volume volume = createVolumeInStoragePool(pool, instance, dVol);
            String action;
            if (CommonStatesConstants.REQUESTED.equals(volume.getState())) {
                action = "Created";
                processManager.create(volume, null);
            } else {
                action = "Using existing";
            }
            log.debug("{} volume [{}] in storage pool [{}].", action, volume.getId(), pool.getId());

            Mount mount = mountVolume(volume, instance, dVol.getContainerPath(), dVol.getAccessMode());
            log.info("Volme mount created. Volume id [{}], instance id [{}], mount id [{}]", volume.getId(), instance.getId(), mount.getId());
            processManager.create(mount, null);
        }
    }

    protected Volume createVolumeInStoragePool(final StoragePool storagePool, final Instance instance, final DockerInspectTransformVolume dVol) {
        Volume volume = volumeDao.getVolumeInPoolByExternalId(dVol.getExternalId(), storagePool);
        if (volume != null)
            return volume;

        return lockManager.lock(new DockerStoragePoolVolumeCreateLock(storagePool, dVol.getExternalId()), new LockCallback<Volume>() {
            @Override
            public Volume doWithLock() {
                Volume volume = volumeDao.createVolumeInPool(instance.getAccountId(), dVol.getName(), dVol.getExternalId(),
                        dVol.getDriver(), storagePool, instance.getNativeContainer());
                return volume;
            }
        });
    }

    protected Mount mountVolume(final Volume volume, final Instance instance, final String path, final String permissions) {
        return lockManager.lock(new MountVolumeLock(volume.getId()), new LockCallback<Mount>() {
            @Override
            public Mount doWithLock() {
                Map<Object, Object> criteria = new HashMap<>();
                criteria.put(MOUNT.VOLUME_ID, volume.getId());
                criteria.put(MOUNT.INSTANCE_ID, instance.getId());
                criteria.put(MOUNT.PATH, path);
                criteria.put(MOUNT.REMOVED, null);
                criteria.put(MOUNT.STATE, new Condition(ConditionType.NE, CommonStatesConstants.INACTIVE));
                Mount mount = objectManager.findAny(Mount.class, criteria);

                if (mount != null) {
                    if (!mount.getPath().equalsIgnoreCase(permissions))
                        objectManager.setFields(mount, MOUNT.PERMISSIONS, permissions);
                    return mount;
                }

                return objectManager.create(Mount.class,
                        MOUNT.ACCOUNT_ID, instance.getAccountId(),
                        MOUNT.INSTANCE_ID, instance.getId(),
                        MOUNT.VOLUME_ID, volume.getId(),
                        MOUNT.PATH, path,
                        MOUNT.STATE, CommonStatesConstants.ACTIVE,
                        MOUNT.PERMISSIONS, permissions);
            }
        });
    }

    protected void assignDockerIp(Instance instance) {
        if ("true".equals(fieldString(instance, InstanceConstants.FIELD_MANAGED_IP))) {
            return;
        }

        String dockerIp = fieldString(instance, InstanceConstants.FIELD_DOCKER_IP);
        setField(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS, dockerIp);
    }

    @Override
    public int getExitCode(Instance instance) {
        return transformer.getExitCode(instance);
    }

}
