package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class PortsConstraint extends HardConstraint implements Constraint {

    List<Port> ports;

    long instanceId;

    public PortsConstraint(long instanceId, List<Port> ports) {
        this.ports = ports;
        this.instanceId = instanceId;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        // TODO: Performance improvement. Move more of the filtering into the DB query itself
        List<Port> portsUsedByHost = candidate.getUsedPorts();
        for (Port portUsed : portsUsedByHost) {
            for (Port requestedPort : ports) {
                if (requestedPort.getPublicPort() != null &&
                        requestedPort.getPublicPort().equals(portUsed.getPublicPort()) &&
                        publicIpTheSame(requestedPort, portUsed) &&
                        requestedPort.getProtocol().equals(portUsed.getProtocol())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean publicIpTheSame(Port requestedPort, Port portUsed) {
        if (requestedPort.getPublicIpAddressId() != null) {
            return requestedPort.getPublicIpAddressId().equals(portUsed.getPublicIpAddressId());
        } else {
            String requestedIp = DataAccessor.fields(requestedPort).withKey(PortConstants.FIELD_BIND_ADDR).as(String.class);
            String usedIp = DataAccessor.fields(portUsed).withKey(PortConstants.FIELD_BIND_ADDR).as(String.class);
            return StringUtils.equals(requestedIp, usedIp);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Port port: ports) {
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
