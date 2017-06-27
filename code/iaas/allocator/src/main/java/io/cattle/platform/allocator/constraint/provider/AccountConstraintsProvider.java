package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.AccountConstraint;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.AccountConstants;
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
        String accountId = idFormatter.formatId(AccountConstants.TYPE, attempt.getAccountId()).toString();
        constraints.add(new AccountConstraint(accountId));
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}