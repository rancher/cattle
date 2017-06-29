package io.cattle.platform.backpopulate.impl;

import static io.cattle.platform.core.constants.DockerInstanceConstants.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.object.util.DataAccessor.*;

import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.DockerInstanceConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.PortSpec;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sound.sampled.Port;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackPopulaterImpl implements BackPopulater {

    private static final Logger log = LoggerFactory.getLogger(BackPopulaterImpl.class);

    JsonMapper jsonMapper;
    VolumeDao volumeDao;
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

        String dockerIp = fieldString(instance, DockerInstanceConstants.FIELD_DOCKER_IP);
        String hostIp = fieldString(host, HostConstants.FIELD_IP_ADDRESS);
        String primaryIp = fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);

        List<String> ports = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_PORTS).as(jsonMapper, List.class);

        assignDockerIp(instance, dockerIp);

        if (hostIpAddress != null && ports != null) {
            processPorts(primaryIp, hostIpAddress, ports, instance, nic, host);
        }

        processVolumes(instance, host);

        processLabels(instance);

        processRemainingFields(instance);

        instanceDao.clearCacheInstanceData(instance.getId());

        return null;
    }

    @SuppressWarnings("unchecked")
    void processLabels(Instance instance) {
        Map<String, Object> inspect = (Map<String, Object>)instance.getData().get(FIELD_DOCKER_INSPECT);
        if (inspect == null) {
            return;
        }
        transformer.setLabels(instance, inspect);
    }

    @SuppressWarnings({ "unchecked" })
    protected void processRemainingFields(Instance instance) {
        Map<String, Object> inspect = (Map<String, Object>)instance.getData().get(FIELD_DOCKER_INSPECT);
        if (inspect == null || instance.getNativeContainer() == null || !instance.getNativeContainer().booleanValue()) {
            return;
        }
        transformer.transform(inspect, instance);
    }

    private Map<String, StoragePool> getPoolsByName(Host host) {
        Map<String, StoragePool> pools = new HashMap<>();
        for (StoragePool pool : objectManager.mappedChildren(host, StoragePool.class)) {
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
        Map<String, Object> inspect = (Map<String, Object>) instance.getData().get(FIELD_DOCKER_INSPECT);
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

        StoragePool dockerLocalStoragePool = null;
        Map<String, StoragePool> pools = getPoolsByName(host);

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

    protected void assignDockerIp(Instance instance, String dockerIp) {
        if ("true".equals(fieldString(instance, InstanceConstants.FIELD_MANAGED_IP))) {
            return;
        }

        setField(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS, dockerIp);
    }

    protected void processPorts(IpAddress primaryIp, IpAddress ipAddress, List<String> ports, Instance instance, Nic nic, final Host host) {
        Long privateIpAddressId = primaryIp == null ? null : primaryIp.getId();

        Map<Integer, Port> existing = new HashMap<>();
        for (Port port : getObjectManager().children(instance, Port.class)) {
            existing.put(port.getPrivatePort(), port);
        }

        Long publicIpAddressId = ipAddress == null ? null : ipAddress.getId();

        for (String entry : ports) {
            PortSpec spec = new PortSpec(entry);
            Port port = existing.get(spec.getPrivatePort());

            if (port == null) {
                Port portObj = objectManager.newRecord(Port.class);
                portObj.setAccountId(instance.getAccountId());
                portObj.setKind(PortConstants.KIND_IMAGE);
                portObj.setInstanceId(instance.getId());
                portObj.setPublicPort(spec.getPublicPort());
                portObj.setPrivatePort(spec.getPrivatePort());
                portObj.setProtocol(spec.getProtocol());
                if (StringUtils.isNotEmpty(spec.getIpAddress()) && !"0.0.0.0".equals(spec.getIpAddress())) {
                    DataAccessor.fields(portObj).withKey(PortConstants.FIELD_BIND_ADDR).set(spec.getIpAddress());
                } else {
                    portObj.setPublicIpAddressId(publicIpAddressId);
                }
                portObj.setPrivateIpAddressId(privateIpAddressId);
                portObj = objectManager.create(portObj);
            } else {
                String bindAddress = DataAccessor.fields(port).withKey(PortConstants.FIELD_BIND_ADDR).as(String.class);
                boolean bindAddressNull = bindAddress == null;
                if (!Objects.equals(port.getPublicPort(), spec.getPublicPort())
                        || !Objects.equals(port.getPrivateIpAddressId(), privateIpAddressId)
                        || (bindAddressNull && !Objects.equals(port.getPublicIpAddressId(), publicIpAddressId))
                        || (!bindAddressNull && !bindAddress.equals(spec.getIpAddress()))){
                    if (spec.getPublicPort() != null) {
                        port.setPublicPort(spec.getPublicPort());
                    }
                    port.setPrivateIpAddressId(privateIpAddressId);
                    if (StringUtils.isNotEmpty(spec.getIpAddress()) && !"0.0.0.0".equals(spec.getIpAddress())) {
                        DataAccessor.fields(port).withKey(PortConstants.FIELD_BIND_ADDR).set(spec.getIpAddress());
                    } else {
                        port.setPublicIpAddressId(publicIpAddressId);
                    }
                    objectManager.persist(port);
                }
            }
        }

        List<String> publishedPorts = new ArrayList<>();
        for (Port port : getObjectManager().children(instance, Port.class)) {
            if (port.getRemoved() != null) {
                continue;
            }
            createIgnoreCancel(port, null);
            if (port.getPublicPort() != null) {
                publishedPorts.add(new PortSpec(port).toSpec());
            }
        }
        List<String> userPorts = DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_PORTS);
        if (DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_USER_PORTS).isEmpty()) {
            DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_USER_PORTS).set(userPorts);
        }
        DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_PORTS).set(publishedPorts);
        objectManager.persist(instance);
    }

    @Override
    public int getExitCode(Instance instance) {
        return transformer.getExitCode(instance);
    }

}
