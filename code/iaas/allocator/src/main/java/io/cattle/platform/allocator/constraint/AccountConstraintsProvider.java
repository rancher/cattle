package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;

import javax.inject.Inject;

public class AccountConstraintsProvider implements AllocationConstraintsProvider {

    @Inject
    ObjectManager objectManager;
    
    @Inject
    IdFormatter idFormatter;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        constraints.add(new AccountConstraint(attempt.getAccountId(), idFormatter));
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}