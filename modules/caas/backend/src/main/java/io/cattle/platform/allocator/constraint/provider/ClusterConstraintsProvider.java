package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.ClusterConstraint;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;

import java.util.List;

public class ClusterConstraintsProvider implements AllocationConstraintsProvider {

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        constraints.add(new ClusterConstraint(attempt.getClusterId()));
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}