package io.cattle.platform.docker.process.instancehostmap;

import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.docker.constants.DockerInstanceConstants.*;
import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.GenericMapDao;
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
import io.cattle.platform.core.model.VolumeStoragePoolMap;
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
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class DockerPostInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener {

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
    ClusterHostMapDao clusterHostMapDao;
    @Inject
    DockerTransformer transformer;
    @Inject
    LabelsService labelsService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        Host host = getObjectManager().loadResource(Host.class, map.getHostId());

        String dockerIp = DockerProcessUtils.getDockerIp(instance);
        Nic nic = nicDao.getPrimaryNic(instance);

        IpAddress ipAddress = clusterHostMapDao.getIpAddressForHost(host.getId());
        Map ports = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_PORTS).as(jsonMapper, Map.class);

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

    void processLabels(Instance instance) {
        Map<String, String> labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(instance.getData(), FIELD_DOCKER_INSPECT, "Config",
                "Labels"));
        for (Map.Entry<String, String>label : labels.entrySet()) {
            labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), label.getKey(), label.getValue());
        }
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
        if (inspect == null) {
            return;
        }
        List<DockerInspectTransformVolume> dockerVolumes = transformer.transformVolumes(inspect);

        if (dockerVolumes.size() == 0) {
            /* If there are no volumes avoid looking for a pool because one may not exists
             * for this host and we don't want to warn about that
             */
            return;
        }

        StoragePool storagePool = null;
        for (StoragePool pool : objectManager.mappedChildren(host, StoragePool.class)) {
            if (DockerStoragePoolDriver.isDockerPool(pool)) {
                storagePool = pool;
                break;
            }
        }

        if (storagePool == null) {
            log.warn("Could not find docker storage pool for host [{}]. Volumes will not be created.", host.getId());
            return;
        }

        for (DockerInspectTransformVolume dVol : dockerVolumes) {
            String volumeUri = null;
            String driver = dVol.getDriver();
            if (StringUtils.isEmpty(driver) || StringUtils.equals(driver, "local")) {
                volumeUri = String.format(VolumeConstants.URI_FORMAT, dVol.getHostPath());
            } else {
                volumeUri = dVol.getHostPath();
            }
            Volume volume = createVolumeInStoragePool(storagePool, instance, volumeUri, dVol.isBindMount());
            log.debug("Created volume and storage pool mapping. Volume id [{}], storage pool id [{}].", volume.getId(), storagePool.getId());
            createIgnoreCancel(volume, state.getData());

            for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
                if (map.getStoragePoolId().equals(storagePool.getId()))
                    createThenActivate(map, state.getData());
            }

            activate(volume, state.getData());

            Mount mount = mountVolume(volume, instance, dVol.getContainerPath(), dVol.getAccessMode());
            log.info("Volme mount created. Volume id [{}], instance id [{}], mount id [{}]", volume.getId(), instance.getId(), mount.getId());
            createThenActivate(mount, null);
        }
    }

    protected Volume createVolumeInStoragePool(final StoragePool storagePool, final Instance instance, final String volumeUri, final boolean isHostBindMount) {
        Volume volume = dockerDao.getDockerVolumeInPool(volumeUri, storagePool);
        if (volume != null)
            return volume;

        return lockManager.lock(new DockerStoragePoolVolumeCreateLock(storagePool, volumeUri), new LockCallback<Volume>() {
            @Override
            public Volume doWithLock() {
                Volume volume = dockerDao.getDockerVolumeInPool(volumeUri, storagePool);
                if (volume != null)
                    return volume;

                volume = dockerDao.createDockerVolumeInPool(instance.getAccountId(), volumeUri, storagePool, isHostBindMount);

                return volume;
            }
        });
    }

    protected Mount mountVolume(final Volume volume, final Instance instance, final String path, final String permissions) {
        Mount mount = objectManager.findOne(Mount.class, MOUNT.VOLUME_ID, volume.getId(), MOUNT.INSTANCE_ID, instance.getId(), MOUNT.PATH, path);

        if (mount != null) {
            if (!mount.getPath().equalsIgnoreCase(permissions))
                objectManager.setFields(mount, MOUNT.PERMISSIONS, permissions);
            return mount;
        }

        return objectManager.create(Mount.class, MOUNT.ACCOUNT_ID, instance.getAccountId(), MOUNT.INSTANCE_ID, instance.getId(), MOUNT.VOLUME_ID,
                volume.getId(), MOUNT.PATH, path, MOUNT.PERMISSIONS, permissions);
    }

    protected void processDockerIp(Instance instance, Nic nic, String dockerIp) {
        if (nic == null) {
            return;
        }

        IpAddress dockerIpAddress = dockerDao.getDockerIp(dockerIp, instance);

        if (dockerIpAddress == null) {
            dockerIpAddress = ipAddressDao.mapNewIpAddress(nic, IP_ADDRESS.SUBNET_ID, null, IP_ADDRESS.KIND, DockerIpAddressConstants.KIND_DOCKER,
                    IP_ADDRESS.ADDRESS, dockerIp);
        }

        if (dockerIpAddress.getKind().equals(DockerIpAddressConstants.KIND_DOCKER)) {
            if (!dockerIp.equals(dockerIpAddress.getAddress())) {
                getObjectManager().setFields(dockerIpAddress, IP_ADDRESS.ADDRESS, dockerIp);
            }
            createThenActivate(dockerIpAddress, null);
        }
    }

    protected void processPorts(IpAddress ipAddress, Map<String, String> ports, Instance instance, Nic nic, final Host host) {
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

        for (Map.Entry<String, String> entry : ports.entrySet()) {
            PortSpec spec = new PortSpec(entry.getKey());
            Port port = existing.get(spec.getPrivatePort());

            if (port == null) {
                port = getObjectManager().create(Port.class, PORT.ACCOUNT_ID, instance.getAccountId(), PORT.INSTANCE_ID, instance.getId(), PORT.PUBLIC_PORT,
                        entry.getValue(), PORT.PRIVATE_PORT, spec.getPrivatePort(), PORT.PROTOCOL, spec.getProtocol(), PORT.PUBLIC_IP_ADDRESS_ID,
                        publicIpAddressId, PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddressId, PORT.KIND, PortConstants.KIND_IMAGE);
            } else {
                if (hasPortNetworkService(instance.getId())) {
                    if (ObjectUtils.equals(port.getPrivateIpAddressId(), privateIpAddressId)) {
                        getObjectManager().setFields(port, PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddressId);
                    }
                } else {
                    if (!ObjectUtils.equals(port.getPublicPort(), entry.getValue()) || !ObjectUtils.equals(port.getPrivateIpAddressId(), privateIpAddressId)
                            || !ObjectUtils.equals(port.getPublicIpAddressId(), publicIpAddressId)) {
                        getObjectManager().setFields(port, PORT.PUBLIC_PORT, entry.getValue(), PORT.PRIVATE_IP_ADDRESS_ID, privateIpAddressId,
                                PORT.PUBLIC_IP_ADDRESS_ID, publicIpAddressId);
                    }
                }
            }
        }

        for (Port port : getObjectManager().children(instance, Port.class)) {
            createIgnoreCancel(port, null);
        }
    }

    protected boolean hasPortNetworkService(long instanceId) {
        return networkDao.getNetworkService(instanceId, NetworkServiceConstants.KIND_PORT_SERVICE).size() > 0;
    }
}
