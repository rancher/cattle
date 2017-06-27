package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.AccountConstants;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

public class AccountConstraint extends HardConstraint implements Constraint {
    long accountId;
    IdFormatter idFormatter;

    public AccountConstraint(long accountId, IdFormatter idFormatter) {
        this.accountId = accountId;
        this.idFormatter = idFormatter;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        // This constraint and doesn't need to  do any matching since the query will limit allocation based on account id.
        return true;
    }

    @Override
    public String toString() {
        return String.format("account id must be %s", idFormatter.formatId(AccountConstants.TYPE, accountId).toString());
    }
}
