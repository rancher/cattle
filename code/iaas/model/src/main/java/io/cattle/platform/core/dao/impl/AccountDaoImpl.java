package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
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
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.tables.records.GenericObjectRecord;
import io.cattle.platform.core.model.tables.records.ProjectMemberRecord;
import io.cattle.platform.core.model.tables.records.UserPreferenceRecord;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.impl.DSL;

@Named
public class AccountDaoImpl extends AbstractCoreDao implements AccountDao {

    @Inject
    ObjectProcessManager objectProcessManager;

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
                                .and(ACCOUNT.STATE.ne(AccountConstants.STATE_PURGED))
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
                        .and(ACCOUNT.STATE.eq(AccountConstants.STATE_PURGED))
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
                        .and(ACCOUNT.STATE.eq(AccountConstants.STATE_PURGED))
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
                .where(ACCOUNT.STATE.eq(AccountConstants.STATE_PURGED)
                        .and(PROJECT_MEMBER.REMOVED.isNull())
                        .and(PROJECT_MEMBER.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(ProjectMemberRecord.class);
    }

    @Override
    public void generateAccountLinks(Account account, List<? extends Long> links) {
        createNewAccountLinks(account, links);
        deleteOldAccountLinks(account, links);
    }

    protected void createNewAccountLinks(Account account, List<? extends Long> newAccountIds) {
        for (Long accountId : newAccountIds) {
            AccountLink link = objectManager.findAny(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                    account.getId(),
                    ACCOUNT_LINK.LINKED_ACCOUNT_ID, accountId,
                    ACCOUNT_LINK.REMOVED, null);
            if (link == null) {
                link = objectManager.create(AccountLink.class, ACCOUNT_LINK.ACCOUNT_ID,
                        account.getId(), ACCOUNT_LINK.LINKED_ACCOUNT_ID, accountId);
            }
            if (link.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessManager.executeStandardProcess(StandardProcess.CREATE, link, null);
            }
        }
    }

    protected void deleteOldAccountLinks(Account account, List<? extends Long> newAccountIds) {
        List<? extends AccountLink> allLinks = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.ACCOUNT_ID, account.getId(),
                ACCOUNT_LINK.REMOVED, null);
        for (AccountLink link : allLinks) {
            if (!newAccountIds.contains(link.getLinkedAccountId())) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, link, null);
            }
        }
    }

    @Override
    public List<Long> getLinkedAccounts(long accountId) {
        List<Long> accountIds = new ArrayList<>();
        List<Long> linkedToAccounts = Arrays.asList(create().select(ACCOUNT_LINK.LINKED_ACCOUNT_ID)
                .from(ACCOUNT_LINK)
                .where(ACCOUNT_LINK.ACCOUNT_ID.eq(accountId)
                        .and(ACCOUNT_LINK.REMOVED.isNull()))
                .fetch().intoArray(ACCOUNT_LINK.LINKED_ACCOUNT_ID));

        List<Long> linkedFromAccounts = Arrays.asList(create().select(ACCOUNT_LINK.ACCOUNT_ID)
                .from(ACCOUNT_LINK)
                .where(ACCOUNT_LINK.LINKED_ACCOUNT_ID.eq(accountId)
                        .and(ACCOUNT_LINK.REMOVED.isNull()))
                .fetch().intoArray(ACCOUNT_LINK.ACCOUNT_ID));

        accountIds.addAll(linkedToAccounts);
        accountIds.addAll(linkedFromAccounts);
        return accountIds;
    }

    @Override
    public Long incrementRevision(long accountId) {
        Record1<Long> row = create().select(ACCOUNT.REVISION)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(accountId))
                .fetchAny();
        if (row == null) {
            return 0L;
        }

        create().update(ACCOUNT)
            .set(ACCOUNT.REVISION, ACCOUNT.REVISION.plus(1))
            .where(ACCOUNT.ID.eq(accountId))
            .execute();

        return row.value1() + 1;
    }
}
