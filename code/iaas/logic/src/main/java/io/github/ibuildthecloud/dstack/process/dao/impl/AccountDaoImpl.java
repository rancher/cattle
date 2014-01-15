package io.github.ibuildthecloud.dstack.process.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.process.dao.AccountDao;

public class AccountDaoImpl extends AbstractJooqDao implements AccountDao {

    @Override
    public Account findByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.UUID.eq(uuid))
                .fetchOne();
    }
}

