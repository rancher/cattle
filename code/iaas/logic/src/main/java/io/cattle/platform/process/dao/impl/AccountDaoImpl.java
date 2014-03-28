package io.cattle.platform.process.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.process.dao.AccountDao;

public class AccountDaoImpl extends AbstractJooqDao implements AccountDao {

    @Override
    public Account findByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.UUID.eq(uuid))
                .fetchOne();
    }
}

