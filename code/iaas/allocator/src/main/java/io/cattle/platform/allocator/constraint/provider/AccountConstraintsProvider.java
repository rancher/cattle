package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.AccountConstraint;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class AccountConstraintsProvider implements AllocationConstraintsProvider {

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        constraints.add(new AccountConstraint(attempt.getAccountId()));
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}