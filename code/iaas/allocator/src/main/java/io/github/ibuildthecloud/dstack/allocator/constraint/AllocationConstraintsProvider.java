package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationLog;

import java.util.List;

public interface AllocationConstraintsProvider {

    public void appendConstraints(AllocationAttempt attempt, AllocationLog log,
            List<Constraint> constraints);

}
