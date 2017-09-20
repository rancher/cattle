package io.cattle.platform.allocator.port;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.metadata.MetadataManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.*;

public class PortManagerImpl implements PortManager {

    Cache<Long, Set<PortInstance>> cache = CacheBuilder.newBuilder()
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
        Set<PortInstance> portsUsedByHost = getPorts(clusterId, hostId);
        if (portsUsedByHost == null || portsUsedByHost.size() == 0) {
            return true;
        }

        for (PortInstance portUsed : portsUsedByHost) {
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
        Set<PortInstance> portMap = getPorts(clusterId, hostId);
        portMap.addAll(ports);
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
        Set<PortInstance> portMap = getPorts(clusterId, hostId);
        portMap.removeAll(ports);
    }

    private Set<PortInstance> getPorts(long clusterId, long hostId) {
        try {
            return cache.get(hostId, () ->
                metadataManager.getMetadataForCluster(clusterId)
                        .getInstances().stream()
                        .filter(instance -> Objects.equals(hostId, instance.getHostId()))
                        .flatMap(instanceInfo -> instanceInfo.getPorts().stream())
                        .collect(toSet()));
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
