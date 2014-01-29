package io.github.ibuildthecloud.dstack.core.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;
import io.github.ibuildthecloud.dstack.core.constants.AccountConstants;
import io.github.ibuildthecloud.dstack.core.dao.AccountDao;
import io.github.ibuildthecloud.dstack.core.model.Account;

public class AccountDaoImpl extends AbstractCoreDao implements AccountDao {

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