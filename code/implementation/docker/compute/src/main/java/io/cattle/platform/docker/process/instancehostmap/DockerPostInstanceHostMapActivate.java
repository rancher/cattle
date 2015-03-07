package io.cattle.platform.docker.process.instancehostmap;

import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.MountTable.MOUNT;
import static io.cattle.platform.core.model.tables.PortTable.PORT;
import static io.cattle.platform.docker.constants.DockerVolumeConstants.READ_ONLY;
import static io.cattle.platform.docker.constants.DockerVolumeConstants.READ_WRITE;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.constants.VolumeConstants;
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
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class DockerPostInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener {

    public static final DynamicBooleanProperty DYNAMIC_ADD_IP = ArchaiusUtil.getBoolean("docker.compute.auto.add.host.ip");

    private static final Logger log = LoggerFactory.getLogger(DockerPostInstanceHostMapActivate.class);

    JsonMapper jsonMapper;
    IpAddressDao ipAddressDao;
    DockerComputeDao dockerDao;
    NicDao nicDao;
    NetworkDao networkDao;
    GenericMapDao mapDao;
    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap) state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        Host host = getObjectManager().loadResource(Host.class, map.getHostId());

        String dockerIp = DockerProcessUtils.getDockerIp(instance);
        Nic nic = nicDao.getPrimaryNic(instance);

        String hostIp = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_HOST_IP).as(String.class);

        Map ports = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_DOCKER_PORTS).as(jsonMapper, Map.class);

        if (dockerIp != null) {
            processDockerIp(instance, nic, dockerIp);
        }

        if (hostIp != null && ports != null) {
            processPorts(hostIp, ports, instance, nic, host);
        }

        processVolumes(instance, host, state);

        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processVolumes(Instance instance, Host host, ProcessState state) {
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

        Map<String, Object> inspect = (Map<String, Object>) instance.getData().get("dockerInspect");
        Map<String, String> volumes = (Map<String, String>) inspect.get("Volumes");
        Map<String, Boolean> volumesRW = (Map<String, Boolean>) inspect.get("VolumesRW");
        Map<String, String> hostBindMounts = extractHostBindMounts((List<String>) ((Map) inspect.get("HostConfig")).get("Binds"));

        for (Map.Entry<String, String> volumeKV : volumes.entrySet()) {
            String pathInContainer = volumeKV.getKey();
            String pathOnHost = volumeKV.getValue();

            String volumeUri = String.format(VolumeConstants.URI_FORMAT, pathOnHost);
            boolean isHostPath = hostBindMounts.containsKey(pathInContainer);
            Volume volume = createVolumeInStoragePool(storagePool, instance, volumeUri, isHostPath);
            log.debug("Created volume and storage pool mapping. Volume id [{}], storage pool id [{}].", volume.getId(), storagePool.getId());
            createIgnoreCancel(volume, state.getData());

            for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
                if (map.getStoragePoolId().equals(storagePool.getId()))
                    createThenActivate(map, state.getData());
            }

            activate(volume, state.getData());

            Boolean readWrite = volumesRW.get(pathInContainer);
            String perms = readWrite ? READ_WRITE : READ_ONLY;
            Mount mount = mountVolume(volume, instance, pathInContainer, perms);
            log.info("Volme mount created. Volume id [{}], instance id [{}], mount id [{}]", volume.getId(), instance.getId(), mount.getId());
            createThenActivate(mount, null);
        }
    }

    private Map<String, String> extractHostBindMounts(List<String> bindMounts) {
        Map<String, String> hostBindMounts = new HashMap<String, String>();
        if (bindMounts == null)
            return hostBindMounts;

        for (String bindMount : bindMounts) {
            String[] parts = bindMount.split(":");
            hostBindMounts.put(parts[1], parts[0]);
        }
        return hostBindMounts;
    }

    protected Volume createVolumeInStoragePool(final StoragePool storagePool, final Instance instance, final String volumeUri, final boolean isHostPath) {
        Volume volume = dockerDao.getDockerVolumeInPool(volumeUri, storagePool);
        if (volume != null)
            return volume;

        return lockManager.lock(new DockerStoragePoolVolumeCreateLock(storagePool, volumeUri), new LockCallback<Volume>() {
            @Override
            public Volume doWithLock() {
                Volume volume = dockerDao.getDockerVolumeInPool(volumeUri, storagePool);
                if (volume != null)
                    return volume;

                volume = dockerDao.createDockerVolumeInPool(instance.getAccountId(), volumeUri, storagePool, isHostPath);

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

        return objectManager.create(Mount.class, MOUNT.ACCOUNT_ID, instance.getAccountId(), MOUNT.INSTANCE_ID, instance.getId(), MOUNT.VOLUME_ID, volume
                .getId(), MOUNT.PATH, path, MOUNT.PERMISSIONS, permissions);
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

    protected void processPorts(final String hostIp, Map<String, String> ports, Instance instance, Nic nic, final Host host) {
        IpAddress ipAddress = getIpAddress(host, hostIp);
        IpAddress dockerIpAddress = dockerDao.getDockerIp(DockerProcessUtils.getDockerIp(instance), instance);
        Long privateIpAddressId = dockerIpAddress == null ? null : dockerIpAddress.getId();

        if (ipAddress == null && DYNAMIC_ADD_IP.get()) {
            ipAddress = lockManager.lock(new AssignHostIpLockDefinition(host), new LockCallback<IpAddress>() {
                @Override
                public IpAddress doWithLock() {
                    IpAddress ipAddress = getIpAddress(host, hostIp);
                    return ipAddress == null ? ipAddressDao.assignNewAddress(host, hostIp) : ipAddress;
                }
            });
        }

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

    protected IpAddress getIpAddress(Host host, String hostIp) {
        for (IpAddress address : getObjectManager().mappedChildren(host, IpAddress.class)) {
            if (hostIp.equals(address.getAddress())) {
                return address;
            }
        }

        return null;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

    public DockerComputeDao getDockerDao() {
        return dockerDao;
    }

    @Inject
    public void setDockerDao(DockerComputeDao dockerDao) {
        this.dockerDao = dockerDao;
    }

    public NicDao getNicDao() {
        return nicDao;
    }

    @Inject
    public void setNicDao(NicDao nicDao) {
        this.nicDao = nicDao;
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
