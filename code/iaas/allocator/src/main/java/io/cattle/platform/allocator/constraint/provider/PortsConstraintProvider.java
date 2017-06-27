package io.cattle.platform.allocator.constraint.provider;

import static io.cattle.platform.core.model.tables.PortTable.*;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.Port;

public class PortsConstraintProvider implements AllocationConstraintsProvider {

    ObjectManager objectManager;

    public PortsConstraintProvider(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Set<String> duplicatePorts = new HashSet<>();
        boolean checkForDupes = attempt.getInstances().size() > 1;
        for (Instance instance : attempt.getInstances()) {
            List<Port> ports = objectManager.find(Port.class, PORT.INSTANCE_ID, instance.getId(), PORT.REMOVED, null);
            if (checkForDupes) {
                for (Port port : ports) {
                    String p = String.format("%s/%s", port.getPublicPort(), port.getProtocol());
                    if (!duplicatePorts.add(p)) {
                        throw new FailedToAllocate(String.format("Port %s requested more than once.", p));
                    }
                }
            }
            if (ports.size() > 0) {
                constraints.add(new PortsConstraint(instance.getId(), ports));
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
