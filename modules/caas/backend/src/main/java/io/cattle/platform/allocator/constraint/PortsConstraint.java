package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.port.PortManager;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.util.PortSpec;

import java.util.List;

public class PortsConstraint extends HardConstraint implements Constraint {

    List<PortSpec> ports;
    PortManager portManager;

    long instanceId;

    public PortsConstraint(long instanceId, List<PortSpec> ports, PortManager portManager) {
        this.ports = ports;
        this.instanceId = instanceId;
        this.portManager = portManager;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        return portManager.portsFree(candidate.getClusterId(), candidate.getHost(), ports);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PortSpec port: ports) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(port.getPublicPort());
            sb.append("/");
            sb.append(port.getProtocol());
        }
        return String.format("host needs ports %s available", sb.toString());
    }
}
