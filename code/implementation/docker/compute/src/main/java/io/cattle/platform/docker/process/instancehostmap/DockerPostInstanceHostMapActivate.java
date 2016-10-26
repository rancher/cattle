package io.cattle.platform.docker.process.instancehostmap;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerIpAddressConstants;
import io.cattle.platform.docker.process.dao.DockerComputeDao;
import io.cattle.platform.docker.process.lock.DockerStoragePoolVolumeCreateLock;
import io.cattle.platform.docker.process.util.DockerProcessUtils;
import io.cattle.platform.docker.storage.DockerStoragePoolDriver;
import io.cattle.platform.docker.transform.DockerInspectTransformVolume;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.lock.MountVolumeLock;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class DockerPostInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    public static final DynamicBooleanProperty DYNAMIC_ADD_IP = ArchaiusUtil.getBoolean("docker.compute.auto.add.host.ip");

    private static final Logger log = LoggerFactory.getLogger(DockerPostInstanceHostMapActivate.class);

    @Inject
    JsonMapper jsonMapper;
    @Inject
    IpAddressDao ipAddressDao;
    @Inject
    DockerComputeDao dockerDao;
    @Inject
    NicDao nicDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    GenericMapDao mapDao;
    @Inject
    LockManager lockManager;
    @Inject
    HostDao hostDao;
    @Inject
    DockerTransformer transformer;
    @Inject
    LabelsService labelsService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @SuppressWarnings("unchecked")
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        Host host = getObjectManager().loadResource(Host.class, map.getHostId());

        String dockerIp = DockerProcessUtils.getDockerIp(instance);
        Nic nic = nicDao.getPrimaryNic(instance);

        IpAddress ipAddress = hostDao.getIpAddressForHost(host.getId());
        List<String> ports = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_PORTS).as(jsonMapper, List.class);

        if (dockerIp != null) {
            processDockerIp(instance, nic, dockerIp);
        }

        if (ipAddress != null && ports != null) {
            processPorts(ipAddress, ports, instance, nic, host);
        }

        processVolumes(instance, host, state);

        processLabels(instance);

        nativeDockerBackPopulate(instance);

        return null;
    }

    @SuppressWarnings("unchecked")
    void processLabels(Instance instance) {
        Map<String, String> labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(instance.getData(), FIELD_DOCKER_INSPECT, "Config",
                "Labels"));
        for (Map.Entry<String, String>label : labels.entrySet()) {
            labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), label.getKey(), label.getValue());
        }

        Map<String, Object> inspect = (Map<String, Object>)instance.getData().get(FIELD_DOCKER_INSPECT);
        if (inspect == null) {
            return;
        }
        transformer.setLabels(instance, inspect);
        objectManager.persist(instance);
    }

    @SuppressWarnings({ "unchecked" })
    protected void nativeDockerBackPopulate(Instance instance) {
        Map<String, Object> inspect = (Map<String, Object>)instance.getData().get(FIELD_DOCKER_INSPECT);
        if (inspect == null || instance.getNativeContainer() == null || !instance.getNativeContainer().booleanValue()) {
            return;
        }
        transformer.transform(inspect, instance);
        objectManager.persist(instance);
    }

    @SuppressWarnings({ "unchecked" })
    protected void processVolumes(Instance instance, Host host, ProcessState state) {

        Map<String, Object> inspect = (Map<String, Object>) instance.getData().get(FIELD_DOCKER_INSPECT);
        List<Object> mounts = (List<Object>) instance.getData().get(FIELD_DOCKER_MOUNTS);
        if (inspect == null && mounts == null) {
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
        Map<String, StoragePool> pools = new HashMap<String, StoragePool>();
        for (StoragePool pool : objectManager.mappedChildren(host, StoragePool.class)) {
            if (DockerStoragePoolDriver.isDockerPool(pool) &&
                    (VolumeConstants.LOCAL_DRIVER.equals(pool.getDriverName()) || StringUtils.isEmpty(pool.getDriverName()))) {
                dockerLocalStoragePool = pool;
            }
            if (StringUtils.isNotEmpty(pool.getDriverName())) {
                pools.put(pool.getDriverName(), pool);
            }
        }

        for (DockerInspectTransformVolume dVol : dockerVolumes) {
            String driver = dVol.getDriver();
            StoragePool pool = null;
            if (driver != null) {
                pool = pools.get(driver);
            }

            if (pool == null) {
                pool = dockerLocalStoragePool;
            }

            Volume volume = createVolumeInStoragePool(pool, instance, dVol);
            String action;
            if (CommonStatesConstants.REMOVED.equals(volume.getState())) {
                action = "Restored";
                objectProcessManager.scheduleStandardProcess(StandardProcess.RESTORE, volume,
                        ProcessUtils.chainInData(state.getData(), "volume.restore", "volume.activate"));
            } else if (CommonStatesConstants.REQUESTED.equals(volume.getState())) {
                action = "Created";
                objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, volume, state.getData());
            } else {
                action = "Using existing";
            }
            log.debug("{} volume [{}] in storage pool [{}].", action, volume.getId(), pool.getId());

            Mount mount = mountVolume(volume, instance, dVol.getContainerPath(), dVol.getAccessMode());
            log.info("Volme mount created. Volume id [{}], instance id [{}], mount id [{}]", volume.getId(), instance.getId(), mount.getId());
            objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, mount, null);
        }
    }

    protected Volume createVolumeInStoragePool(final StoragePool storagePool, final Instance instance, final DockerInspectTransformVolume dVol) {
        Volume volume = dockerDao.getDockerVolumeInPool(dVol.getUri(), dVol.getExternalId(), storagePool);
        if (volume != null)
            return volume;

        return lockManager.lock(new DockerStoragePoolVolumeCreateLock(storagePool, dVol.getExternalId()), new LockCallback<Volume>() {
            @Override
            public Volume doWithLock() {
                Volume volume = dockerDao.createDockerVolumeInPool(instance.getAccountId(), dVol.getName(), dVol.getUri(), dVol.getExternalId(),
                        dVol.getDriver(), storagePool, dVol.isBindMount());
                return volume;
            }
        });
    }

    protected Mount mountVolume(final Volume volume, final Instance instance, final String path, final String permissions) {
        return lockManager.lock(new MountVolumeLock(volume.getId()), new LockCallback<Mount>() {
            @Override
            public Mount doWithLock() {
                Map<Object, Object> criteria = new HashMap<Object, Object>();
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

                return objectManager.create(Mount.class, MOUNT.ACCOUNT_ID, instance.getAccountId(), MOUNT.INSTANCE_ID, instance.getId(), MOUNT.VOLUME_ID,
                        volume.getId(), MOUNT.PATH, path, MOUNT.PERMISSIONS, permissions);
            }
        });
    }

    protected void processDockerIp(Instance instance, Nic nic, String dockerIp) {
        if (nic == null) {
            return;
        }

        IpAddress dockerIpAddress = dockerDao.getDockerIp(dockerIp, instance);

        if (dockerIpAddress == null) {
            dockerIpAddress = ipAddressDao.mapNewIpAddress(nic,
                    IP_ADDRESS.KIND, DockerIpAddressConstants.KIND_DOCKER,
                    IP_ADDRESS.ROLE, IpAddressConstants.ROLE_PRIMARY,
                    IP_ADDRESS.ADDRESS, dockerIp);
        }

        if (dockerIpAddress.getKind().equals(DockerIpAddressConstants.KIND_DOCKER)) {
            if (!dockerIp.equals(dockerIpAddress.getAddress())) {
                getObjectManager().setFields(dockerIpAddress, IP_ADDRESS.ADDRESS, dockerIp);
            }
            createThenActivate(dockerIpAddress, null);
        }
    }

    protected void processPorts(IpAddress ipAddress, List<String> ports, Instance instance, Nic nic, final Host host) {
        IpAddress dockerIpAddress = dockerDao.getDockerIp(DockerProcessUtils.getDockerIp(instance), instance);
        Long privateIpAddressId = dockerIpAddress == null ? null : dockerIpAddress.getId();

        if (DYNAMIC_ADD_IP.get()) {
            createThenActivate(ipAddress, null);
            for (HostIpAddressMap map : getObjectManager().children(ipAddress, HostIpAddressMap.class)) {
                if (map.getHostId().longValue() == host.getId()) {
                    createThenActivate(map, null);
                }
            }
        }

        Map<Integer, Port> existing = new HashMap<Integer, Port>();
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
                if (!ObjectUtils.equals(port.getPublicPort(), spec.getPublicPort())
                        || !ObjectUtils.equals(port.getPrivateIpAddressId(), privateIpAddressId)
                        || (bindAddressNull && !ObjectUtils.equals(port.getPublicIpAddressId(), publicIpAddressId))
                        || (!bindAddressNull && !bindAddress.equals(spec.getIpAddress()))){
                    port.setPublicPort(spec.getPublicPort());
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

        for (Port port : getObjectManager().children(instance, Port.class)) {
            createIgnoreCancel(port, null);
        }
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }
}
