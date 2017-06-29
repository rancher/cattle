package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;

import java.util.List;

public interface AllocationConstraintsProvider {

    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints);

    public boolean isCritical();

}
