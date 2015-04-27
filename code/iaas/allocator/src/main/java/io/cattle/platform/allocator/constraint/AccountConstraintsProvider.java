package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class AccountConstraintsProvider implements AllocationConstraintsProvider {

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Long accountId = null;

        Instance instance = attempt.getInstance();
        if (instance != null) {
            accountId = instance.getAccountId();
        }

        if (accountId == null) {
            for (Volume v : attempt.getVolumes()) {
                accountId = v.getAccountId();
                break;
            }
        }

        if (accountId != null) {
            constraints.add(new AccountConstraint(objectManager, accountId));
        }
    }

}