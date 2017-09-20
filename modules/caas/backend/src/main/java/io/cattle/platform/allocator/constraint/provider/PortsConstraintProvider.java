package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.port.PortManager;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.object.ObjectManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PortsConstraintProvider implements AllocationConstraintsProvider {

    ObjectManager objectManager;
    PortManager portManager;

    public PortsConstraintProvider(ObjectManager objectManager, PortManager portManager) {
        this.objectManager = objectManager;
        this.portManager = portManager;
    }

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Set<String> duplicatePorts = new HashSet<>();
        boolean checkForDupes = attempt.getInstances().size() > 1;
        for (Instance instance : attempt.getInstances()) {
            List<PortSpec> ports = InstanceConstants.getPortSpecs(instance);
            if (checkForDupes) {
                for (PortSpec port : ports) {
                    String p = String.format("%s/%s", port.getPublicPort(), port.getProtocol());
                    if (!duplicatePorts.add(p)) {
                        throw new FailedToAllocate(String.format("Port %s requested more than once.", p));
                    }
                }
            }
            if (ports.size() > 0) {
                constraints.add(new PortsConstraint(instance.getId(), ports, portManager));
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
