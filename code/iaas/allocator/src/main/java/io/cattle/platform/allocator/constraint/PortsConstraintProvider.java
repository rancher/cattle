package io.cattle.platform.allocator.constraint;

import static io.cattle.platform.core.model.tables.PortTable.*;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class PortsConstraintProvider implements AllocationConstraintsProvider {

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log,
            List<Constraint> constraints) {
        Instance instance = attempt.getInstance();

        if (instance != null) {
            List<Port> ports = objectManager.find(Port.class, PORT.INSTANCE_ID, instance.getId(), PORT.REMOVED, null);
            if (ports.size() > 0) {
                constraints.add(new PortsConstraint(instance.getId(), ports, allocatorDao));
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
