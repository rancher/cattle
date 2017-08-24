package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;

public class AccountDaoImpl extends AbstractJooqDao implements AccountDao {

    private static final Set<String> GOOD_STATES = CollectionUtils.set(
            CommonStatesConstants.REGISTERING,
            CommonStatesConstants.ACTIVATING,
            CommonStatesConstants.ACTIVE,
            CommonStatesConstants.UPDATING,
            ServiceConstants.STATE_UPGRADING);

    ObjectManager objectManager;

    public AccountDaoImpl(Configuration configuration, ObjectManager objectManager) {
        super(configuration);
        this.objectManager = objectManager;
    }

    @Override
    public List<? extends Credential> getApiKeys(Account account, String kind, boolean active) {
        if (kind == null) {
            kind = CredentialConstants.KIND_API_KEY;
        }

        Condition stateCondition = DSL.trueCondition();
        if ( active ) {
            stateCondition = CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE);
        }

        return create().selectFrom(CREDENTIAL)
            .where(CREDENTIAL.ACCOUNT_ID.eq(account.getId())
                    .and(CREDENTIAL.REMOVED.isNull())
                    .and(stateCondition)
                    .and(CREDENTIAL.KIND.eq(kind)))
            .fetch();
    }

    @Override
    public Account findByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.UUID.eq(uuid))
                .fetchOne();
    }

    @Override
    public void deleteProjectMemberEntries(Account account) {
        if (!ProjectConstants.TYPE.equalsIgnoreCase(account.getKind())
                && StringUtils.isNotBlank(account.getExternalId())
                && StringUtils.isNotBlank(account.getExternalIdType())){
            create().delete(PROJECT_MEMBER)
                    .where(PROJECT_MEMBER.EXTERNAL_ID.eq(account.getExternalId())
                            .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(account.getExternalIdType())))
                    .execute();
        }
        create().delete(PROJECT_MEMBER)
            .where(PROJECT_MEMBER.PROJECT_ID.eq(account.getId()))
            .execute();
    }

    @Override
    public Account getAdminAccountExclude(long accountId) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.STATE.in(getAccountActiveStates())
                        .and(ACCOUNT.KIND.eq(AccountConstants.ADMIN_KIND))
                        .and(ACCOUNT.ID.ne(accountId)))
                .orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }


    @Override
    public Account getAccountById(Long id) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.ID.eq(id)
                                .and(ACCOUNT.STATE.ne(AccountConstants.STATE_PURGED))
                                .and(ACCOUNT.REMOVED.isNull())
                ).fetchOne();
    }

    @Override
    public boolean isActiveAccount(Account account) {
        return GOOD_STATES.contains(account.getState());
    }

    @Override
    public List<String> getAccountActiveStates() {
        return Arrays.asList(CommonStatesConstants.ACTIVE, ServiceConstants.STATE_UPGRADING);
    }

}
