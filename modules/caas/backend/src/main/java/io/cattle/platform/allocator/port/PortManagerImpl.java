package io.cattle.platform.allocator.port;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.metadata.MetadataManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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
    public void assignPorts(long clusterId, long hostId, Collection<PortInstance> ports) {
        Map<String, PortInstance> portMap = getPorts(clusterId, hostId);
        for (PortInstance port : ports) {
            portMap.put(toKey(port), port);
        }
    }

    @Override
    public boolean optionallyAssignPorts(long clusterId, long hostId, long instanceId, Collection<PortInstance> ports) {
        List<PortSpec> specs = ports.stream()
                .map(PortSpec::new)
                .collect(toList());

        if (specs.size() == 0) {
            return true;
        }

        if (!portsFree(clusterId, hostId, instanceId, specs)) {
            return false;
        }

        assignPorts(clusterId, hostId, ports);
        return true;
    }

    @Override
    public void releasePorts(long clusterId, long hostId, Collection<PortInstance> ports) {
        Map<String, PortInstance> portMap = getPorts(clusterId, hostId);
        for (PortInstance port : ports) {
            portMap.remove(toKey(port));
        }
    }

    private Map<String, PortInstance> getPorts(long clusterId, long hostId) {
        try {
            return cache.get(hostId, () ->
                metadataManager.getMetadataForCluster(clusterId)
                        .getInstances().stream()
                        .filter(instance -> Objects.equals(hostId, instance.getHostId()) &&
                                InstanceConstants.STATE_RUNNING.equals(instance.getState()))
                        .flatMap(instanceInfo -> instanceInfo.getPorts().stream())
                        .collect(Collectors.toConcurrentMap(
                                this::toKey,
                                Function.identity())));
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toKey(PortInstance port) {
        return String.format("%s:%s/%s", port.getBindIpAddress(), port.getPublicPort(), port.getProtocol());
    }

}
