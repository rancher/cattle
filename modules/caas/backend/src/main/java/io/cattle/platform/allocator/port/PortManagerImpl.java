package io.cattle.platform.allocator.port;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PortManagerImpl implements PortManager {

    Cache<Long, Map<String, PortInstance>> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();
    MetadataManager metadataManager;

    public PortManagerImpl(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    @Override
    public boolean portsFree(long clusterId, long hostId, Collection<PortSpec> ports) {
        return portsFree(clusterId, hostId, null, ports);
    }

    private boolean portsFree(long clusterId, long hostId, Long instanceId, Collection<PortSpec> ports) {
        Map<String, PortInstance> portsUsedByHost = getPorts(clusterId, hostId);
        if (portsUsedByHost == null || portsUsedByHost.size() == 0) {
            return true;
        }

        for (PortInstance portUsed : portsUsedByHost.values()) {
            for (PortSpec requestedPort : ports) {
                if (requestedPort.getPublicPort() != null &&
                        requestedPort.getPublicPort().equals(portUsed.getPublicPort()) &&
                        publicIpTheSame(requestedPort, portUsed) &&
                        requestedPort.getProtocol().equals(portUsed.getProtocol())) {
                    if (instanceId != null && Objects.equals(instanceId, portUsed.getInstanceId())) {
                        continue;
                    }

                    return false;
                }
            }
        }

        return true;
    }


    private boolean publicIpTheSame(PortSpec requestedPort, PortInstance portUsed) {
        if (portUsed.isBindAll()) {
            return true;
        }
        return Objects.equals(requestedPort.getIpAddress(), portUsed.getBindIpAddress());
    }

    @Override
    public void assignPorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> ports) {
        Map<String, PortInstance> portMap = getPorts(clusterId, hostId);
        for (PortSpec port : ports) {
            PortInstance instance = new PortInstance(port, instanceId, hostId);
            portMap.put(toKey(instance), instance);
        }
    }

    @Override
    public boolean optionallyAssignPorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> specs) {
        if (specs.size() == 0) {
            return true;
        }

        if (!portsFree(clusterId, hostId, instanceId, specs)) {
            return false;
        }

        assignPorts(clusterId, hostId, instanceId, specs);
        return true;
    }

    @Override
    public void releasePorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> specs) {
        Map<String, PortInstance> portMap = getPorts(clusterId, hostId);
        for (PortSpec spec : specs) {
            PortInstance port = new PortInstance(spec, instanceId, hostId);
            String key = toKey(port);
            PortInstance existing = portMap.get(key);
            if (existing != null && Objects.equals(port.getInstanceId(), existing.getInstanceId())) {
                portMap.remove(key);
            }
        }
    }

    private Map<String, PortInstance> getPorts(long clusterId, long hostId) {
        try {
            return cache.get(hostId, () -> {
                Metadata metadata = metadataManager.getMetadataForCluster(clusterId);
                Map<String, PortInstance> portSet = getPortSet(metadata.getInstances(), hostId);

                for (EnvironmentInfo info : metadata.getEnvironments()) {
                    metadata = metadataManager.getMetadataForAccount(info.getAccountId());
                    portSet.putAll(getPortSet(metadata.getInstances(), hostId));
                }

                return portSet;
            });
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, PortInstance> getPortSet(Collection<InstanceInfo> instanceInfos, long hostId) {
        Map<String, PortInstance> result = new HashMap<>();

        for (InstanceInfo instanceInfo : instanceInfos) {
            if (!Objects.equals(hostId, instanceInfo.getHostId()) ||
                !InstanceConstants.STATE_RUNNING.equals(instanceInfo.getState())) {
                continue;
            }

            for (String spec : instanceInfo.getPortSpecs()) {
                try {
                    PortInstance port = new PortInstance(new PortSpec(spec), instanceInfo.getId(), instanceInfo.getHostId());
                    result.put(toKey(port), port);
                } catch (ClientVisibleException ignored) {
                }
            }
        }

        return result;
    }

    private String toKey(PortInstance port) {
        return String.format("%s:%s/%s", port.getBindIpAddress(), port.getPublicPort(), port.getProtocol());
    }

}
