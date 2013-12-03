package io.github.ibuildthecloud.dstack.api.auth.dao.impl;

import static io.github.ibuildthecloud.dstack.db.jooq.generated.tables.AccountTable.*;
import io.github.ibuildthecloud.dstack.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.db.jooq.generated.model.Account;

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
