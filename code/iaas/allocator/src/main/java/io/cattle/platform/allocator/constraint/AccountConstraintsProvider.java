package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;

import javax.inject.Inject;

public class AccountConstraintsProvider implements AllocationConstraintsProvider {

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Account account = null;

        Instance instance = attempt.getInstance();
        if (instance != null) {
            account = objectManager.loadResource(Account.class, instance.getAccountId());
        }

        if (account == null) {
            for (Volume v : attempt.getVolumes()) {
                account = objectManager.loadResource(Account.class, v.getAccountId());
                break;
            }
        }

        if (account == null) {
            return;
        }

        Boolean enabled = DataAccessor.fromDataFieldOf(account)
            .withScope(AccountConstraintsProvider.class)
            .withDefault(true)
            .withKey("accountScoped")
            .as(Boolean.class);

        if (enabled) {
            constraints.add(new AccountConstraint(objectManager, account.getId()));
        }
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}