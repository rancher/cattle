package io.github.ibuildthecloud.dstack.api.auth.dao.impl;

import static io.github.ibuildthecloud.dstack.core.tables.AccountTable.*;

import io.github.ibuildthecloud.dstack.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;

public class AuthDaoImpl extends AbstractJooqDao implements AuthDao {

    @Override
    public Account getAdminAccount() {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.STATE.eq("active")
                        .and(ACCOUNT.KIND.eq("admin"))
                ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

}
