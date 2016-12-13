package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.GenericObjectTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;
import static io.cattle.platform.core.model.tables.UserPreferenceTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.tables.records.GenericObjectRecord;
import io.cattle.platform.core.model.tables.records.ProjectMemberRecord;
import io.cattle.platform.core.model.tables.records.UserPreferenceRecord;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

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
                                .and(ACCOUNT.STATE.ne(CommonStatesConstants.PURGED))
                                .and(ACCOUNT.REMOVED.isNull())
                ).fetchOne();
    }

    @Override
    public boolean isActiveAccount(Account account) {
        List<String> goodStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                ServiceConstants.STATE_UPGRADING);
        return goodStates.contains(account.getState());
    }

    @Override
    public List<String> getAccountActiveStates() {
        return Arrays.asList(CommonStatesConstants.ACTIVE, ServiceConstants.STATE_UPGRADING);
    }

    @Override
    public List<? extends GenericObject> findBadGO(int count) {
        return create().select(GENERIC_OBJECT.fields())
                .from(GENERIC_OBJECT)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(GENERIC_OBJECT.ACCOUNT_ID))
                .where(GENERIC_OBJECT.REMOVED.isNull()
                        .and(ACCOUNT.STATE.eq(CommonStatesConstants.PURGED))
                        .and(GENERIC_OBJECT.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.DEACTIVATING)))
                .limit(count)
                .fetchInto(GenericObjectRecord.class);
    }

    @Override
    public List<? extends UserPreference> findBadUserPreference(int count) {
        return create().select(USER_PREFERENCE.fields())
                .from(USER_PREFERENCE)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(USER_PREFERENCE.ACCOUNT_ID))
                .where(USER_PREFERENCE.REMOVED.isNull()
                        .and(ACCOUNT.STATE.eq(CommonStatesConstants.PURGED))
                        .and(USER_PREFERENCE.STATE.notIn(CommonStatesConstants.REMOVING, CommonStatesConstants.DEACTIVATING)))
                .limit(count)
                .fetchInto(UserPreferenceRecord.class);
    }

    @Override
    public List<? extends ProjectMember> findBadProjectMembers(int count) {
        return create().select(PROJECT_MEMBER.fields())
                .from(PROJECT_MEMBER)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(PROJECT_MEMBER.PROJECT_ID))
                .where(ACCOUNT.STATE.eq(CommonStatesConstants.PURGED)
                        .and(PROJECT_MEMBER.REMOVED.isNull())
                        .and(PROJECT_MEMBER.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(ProjectMemberRecord.class);
    }

}
