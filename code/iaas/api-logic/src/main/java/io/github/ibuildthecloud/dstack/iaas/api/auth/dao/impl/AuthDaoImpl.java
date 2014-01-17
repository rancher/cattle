package io.github.ibuildthecloud.dstack.iaas.api.auth.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.CredentialTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;

import com.netflix.config.DynamicStringListProperty;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.model.tables.records.AccountRecord;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.iaas.api.auth.dao.AuthDao;

public class AuthDaoImpl extends AbstractJooqDao implements AuthDao {

    private DynamicStringListProperty SUPPORTED_TYPES = ArchaiusUtil.getList("account.by.key.credential.types");
    @Override
    public Account getAdminAccount() {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.STATE.eq("active")
                        .and(ACCOUNT.KIND.eq("admin"))
                ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

    @Override
    public Account getAccountByKeys(String access, String secretKey) {
        return create()
                .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                .join(CREDENTIAL)
                    .on(CREDENTIAL.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(
                    ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                    .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(CREDENTIAL.PUBLIC_VALUE.eq(access))
                    .and(CREDENTIAL.SECRET_VALUE.eq(secretKey)))
                    .and(CREDENTIAL.KIND.in(SUPPORTED_TYPES.get()))
                .fetchOneInto(AccountRecord.class);
    }

}
