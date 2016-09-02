package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;

public class AccountConstraint extends HardConstraint implements Constraint {
    long accountId;

    public AccountConstraint(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        // This constraint and doesn't need to  do any matching since the query will limit allocation based on account id.
        return true;
    }

    @Override
    public String toString() {
        return String.format("account id must be %d", accountId);
    }
}
