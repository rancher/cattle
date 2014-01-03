package io.github.ibuildthecloud.dstack.core.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;
import io.github.ibuildthecloud.dstack.core.constants.AccountConstants;
import io.github.ibuildthecloud.dstack.core.dao.AccountCoreDao;
import io.github.ibuildthecloud.dstack.core.model.Account;

public class AccountCoreDaoImpl extends AbstractCoreDao implements AccountCoreDao {

    @Override
    public Account getSystemAccount() {
        Account system = objectManager.findOne(Account.class,
                ACCOUNT.UUID, AccountConstants.SYSTEM_UUID);
        if ( system == null ) {
            throw new IllegalStateException("Failed to find system account");
        }

        return system;
    }

}