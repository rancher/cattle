package io.cattle.platform.allocator.port;

import io.cattle.platform.core.util.PortSpec;

import java.util.Collection;

public interface PortManager {

    boolean portsFree(long clusterId, long hostId, Collection<PortSpec> ports);

    void assignPorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> ports);

    boolean optionallyAssignPorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> ports);

    void releasePorts(long clusterId, long hostId, long instanceId, Collection<PortSpec> ports);

}
