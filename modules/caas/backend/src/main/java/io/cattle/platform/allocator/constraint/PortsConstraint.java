package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.util.PortSpec;

import java.util.List;
import java.util.Set;

public class PortsConstraint extends HardConstraint implements Constraint {

    List<PortSpec> ports;

    long instanceId;

    public PortsConstraint(long instanceId, List<PortSpec> ports) {
        this.ports = ports;
        this.instanceId = instanceId;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        // TODO: Performance improvement. Move more of the filtering into the DB query itself
        Set<String> portsUsedByHost = candidate.getUsedPorts();
        for (String portUsed : portsUsedByHost) {
            for (PortSpec requestedPort : ports) {
                if (requestedPort.toSpecConstraint().equals(portUsed)) {
                    return false;
                }
            }
        }
        return true;
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
