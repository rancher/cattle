package io.cattle.platform.iaas.api.auth.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.core.model.tables.records.ProjectMemberRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.projects.ProjectLock;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.projects.Member;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.TableField;
import org.jooq.exception.InvalidResultException;
import org.jooq.impl.DSL;

import com.netflix.config.DynamicStringListProperty;

public class AuthDaoImpl extends AbstractJooqDao implements AuthDao {

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    LockManager lockManager;
    private DynamicStringListProperty SUPPORTED_TYPES = ArchaiusUtil.getList("account.by.key.credential.types");

    @Override
    public Account getAdminAccount() {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(ACCOUNT.KIND.eq(AccountConstants.ADMIN_KIND)))
                .orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

    @Override
    public List<Account> searchUsers(String username) {
        return create()
                .select(ACCOUNT.fields())
                .from(ACCOUNT)
                .join(CREDENTIAL)
                .on(CREDENTIAL.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                        .and(CREDENTIAL.PUBLIC_VALUE.contains(username))
                        .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_PASSWORD)))
                .orderBy(ACCOUNT.ID.asc()).fetchInto(Account.class);
    }

    @Override
    public Account getByUsername(String username) {
        try {
            return create()
                    .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                    .join(CREDENTIAL)
                    .on(CREDENTIAL.ACCOUNT_ID.eq(ACCOUNT.ID))
                    .where(
                            ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                                    .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                                    .and(CREDENTIAL.PUBLIC_VALUE.eq(username)))
                    .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_PASSWORD))
                    .fetchOneInto(AccountRecord.class);
        } catch (InvalidResultException e) {
            throw new ClientVisibleException(ResponseCodes.CONFLICT, "MultipleOfUsername");
        }
    }

    @Override
    public Account getAccountByLogin(String publicValue, String secretValue, TransformationService transformationService) {
        Credential credential = create()
                .selectFrom(CREDENTIAL)
                .where(
                        CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                .and(CREDENTIAL.PUBLIC_VALUE.eq(publicValue)
                        .and(CREDENTIAL.KIND.equalIgnoreCase(CredentialConstants.KIND_PASSWORD)))
                .fetchOne();
        if (credential == null) {
            return null;
        }
        boolean secretIsCorrect = transformationService.compare(secretValue, credential.getSecretValue());
        if (secretIsCorrect) {
            return create()
                    .selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(credential.getAccountId())
                            .and(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)))
                    .fetchOneInto(AccountRecord.class);
        } else {
            return null;
        }
    }

    @Override
    public String getRole(Account account, Policy policy) {
        List<? extends ProjectMember> projectMembers;
        if (account != null && account.getKind().equalsIgnoreCase(ProjectConstants.TYPE)) {
            projectMembers = getProjectMembersByIdentity(account.getId(), policy.getIdentities());
            if (projectMembers == null || projectMembers.size() == 0) {
                return account.getKind();
            } else {
                String role = null;
                for (ProjectMember projectMember : projectMembers) {
                    if (role == null) {
                        role = projectMember.getRole();
                    } else {
                        String newRole = projectMember.getRole();

                        if (getRolePriority(newRole) < getRolePriority(role)) {
                            role = newRole;
                        }
                    }
                }
                return role;
            }
        } else if (account != null){
            return account.getKind();
        } else {
            return null;
        }
    }


    private int getRolePriority(String role) {
        return ArchaiusUtil.getInt(SecurityConstants.ROLE_SETTING_BASE + role).get();
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
    public Account getAccountByAccessKey(String accessKey) {
        return create()
                .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                .join(CREDENTIAL)
                    .on(CREDENTIAL.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(
                        CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                                .and(CREDENTIAL.PUBLIC_VALUE.eq(accessKey))
                                .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_API_KEY))
                                .and(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)))
                .fetchOneInto(AccountRecord.class);
    }

    @Override
    public Account getAccountByKeys(String access, String secretKey, TransformationService transformationService) {
        try {
            Credential credential = create()
                    .selectFrom(CREDENTIAL)
                    .where(
                            CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE))
                                    .and(CREDENTIAL.PUBLIC_VALUE.eq(access))
                                    .and(CREDENTIAL.KIND.in(SUPPORTED_TYPES.get()))
                    .fetchOne();
            if (credential == null) {
                return null;
            }
            if (transformationService.compare(secretKey, credential.getSecretValue())) {
                return create()
                        .selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(credential.getAccountId())
                                .and(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)))
                        .fetchOneInto(AccountRecord.class);
            }
            else {
                return null;
            }
        } catch (InvalidResultException e) {
            throw new ClientVisibleException(ResponseCodes.CONFLICT, "MultipleKeys");
        }
    }

    @Override
    public Account getAccountByExternalId(String externalId, String externalType) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.EXTERNAL_ID.eq(externalId)
                                .and(ACCOUNT.EXTERNAL_ID_TYPE.eq(externalType))
                                .and(ACCOUNT.STATE.ne("purged"))
                ).orderBy(ACCOUNT.ID.asc()).fetchOne();
    }

    @Override
    public Account createAccount(String name, String kind, String externalId, String externalType) {
        Account account = getAccountByExternalId(externalId, externalType);
        if (account != null){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        Map<Object, Object> properties = new HashMap<>();
        if (StringUtils.isNotEmpty(name)) {
            properties.put(ACCOUNT.NAME, name);
        }
        if (StringUtils.isNotEmpty(kind)) {
            properties.put(ACCOUNT.KIND, kind);
        }
        if (StringUtils.isNotEmpty(externalId)) {
            properties.put(ACCOUNT.EXTERNAL_ID, externalId);
        }
        if (StringUtils.isNotEmpty(externalType)) {
            properties.put(ACCOUNT.EXTERNAL_ID_TYPE, externalType);
        }
        return resourceDao.createAndSchedule(Account.class, objectManager.convertToPropertiesFor(Account.class,
                properties));
    }

    @Override
    public Identity getIdentity(Long id, IdFormatter idFormatter) {
        Account account = getAccountById(id);
        if (account == null || account.getKind().equalsIgnoreCase(ProjectConstants.TYPE) ||
                !CommonStatesConstants.ACTIVE.equalsIgnoreCase(account.getState())) {
            return null;
        }
        Credential credential = create()
                .selectFrom(CREDENTIAL)
                .where(CREDENTIAL.KIND.equalIgnoreCase(CredentialConstants.KIND_PASSWORD)
                        .and(CREDENTIAL.ACCOUNT_ID.eq(id))
                        .and(CREDENTIAL.STATE.equalIgnoreCase(CommonStatesConstants.ACTIVE))).fetchAny();
        String accountId = idFormatter != null ? (String) idFormatter.formatId(objectManager.getType(Account.class),
                account.getId()) : String.valueOf(id);
        return new Identity(ProjectConstants.RANCHER_ID, accountId, account.getName(),
                null, null, credential == null ? null : credential.getPublicValue());
    }

    @Override
    public Account createProject(String name, String description) {
        Map<Object, Object> properties = new HashMap<>();
        if (name != null) {
            properties.put(ACCOUNT.NAME, name);
        }
        if (description != null) {
            properties.put(ACCOUNT.DESCRIPTION, description);
        }
        properties.put(ACCOUNT.KIND, ProjectConstants.TYPE);
        return resourceDao.createAndSchedule(Account.class, objectManager.convertToPropertiesFor(Account.class,
                properties));
    }

    @Override
    public Account getAccountByUuid(String uuid) {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.UUID.eq(uuid)
                        .and(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)))
                .orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

    @Override
    public Account updateAccount(Account account, String name, String kind, String externalId, String externalType) {
        Map<TableField<AccountRecord, String>, String> properties = new HashMap<>();
        if (StringUtils.isNotEmpty(name)) {
            properties.put(ACCOUNT.NAME, name);
        }
        if (StringUtils.isNotEmpty(kind)) {
            properties.put(ACCOUNT.KIND, kind);
        }
        if (StringUtils.isNotEmpty(externalId)) {
            properties.put(ACCOUNT.EXTERNAL_ID, externalId);
        }
        if (StringUtils.isNotEmpty(externalType)) {
            properties.put(ACCOUNT.EXTERNAL_ID_TYPE, externalType);
        }
        int updateCount = create()
                .update(ACCOUNT)
                .set(properties)
                .where(ACCOUNT.ID
                        .eq(account.getId()))
                .execute();

        if (1 != updateCount) {
            throw new RuntimeException("UpdateAccount failed.");
        }
        return objectManager.reload(account);
    }

    @Override
    public List<Account> getAccessibleProjects(Set<Identity> identities, boolean isAdmin, Long usingAccount) {
        List<Account> projects = new ArrayList<>();
        if (identities == null) {
            return projects;
        }
        if (isAdmin) {
            projects.addAll(create()
                    .selectFrom(ACCOUNT)
                    .where(ACCOUNT.KIND.eq(ProjectConstants.TYPE)
                            .and(ACCOUNT.REMOVED.isNull()))
                    .orderBy(ACCOUNT.ID.asc())
                    .fetch());
            return projects;
        }

        if (usingAccount != null) {
            Account project = getAccountById(usingAccount);
            if (project != null && project.getKind().equalsIgnoreCase(ProjectConstants.TYPE)) {
                projects.add(project);
                return projects;
            }
        }
        //DSL.falseCondition is created so that we can dynamically build a or
        //Condition without caring what the external Ids are and still make one
        //Database call.
        Condition allMembers = DSL.falseCondition();
        for (Identity id : identities) {
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getExternalId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getExternalIdType()))
                    .and(PROJECT_MEMBER.REMOVED.isNull())
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE)));
        }
        SelectQuery<Record> query = create().selectQuery();
        query.addFrom(ACCOUNT);
        query.addJoin(PROJECT_MEMBER, PROJECT_MEMBER.PROJECT_ID.equal(ACCOUNT.ID));
        query.addConditions(allMembers);
        query.setDistinct(true);
        projects.addAll(query.fetchInto(ACCOUNT));
        Map<Long, Account> returnProjects = new HashMap<>();
        for (Account project : projects) {
            returnProjects.put(project.getId(), project);
        }
        projects = new ArrayList<>();
        projects.addAll(returnProjects.values());
        return projects;
    }

    @Override
    public List<? extends ProjectMember> getActiveProjectMembers(long id) {
        return create()
                .selectFrom(PROJECT_MEMBER)
                .where(PROJECT_MEMBER.PROJECT_ID.eq(id))
                .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                .and(PROJECT_MEMBER.REMOVED.isNull())
                .orderBy(PROJECT_MEMBER.ID.asc()).fetch();
    }

    @Override
    public List<? extends ProjectMember> getProjectMembersByIdentity(long projectId, Set<Identity> identities) {
        Condition allMembers = DSL.falseCondition();
        for (Identity identity : identities) {
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(identity.getExternalId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(identity.getExternalIdType()))
                    .and(PROJECT_MEMBER.REMOVED.isNull())
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(PROJECT_MEMBER.PROJECT_ID.eq(projectId)));
        }
        SelectQuery<Record> query = create().selectQuery();
        query.addFrom(PROJECT_MEMBER);
        query.addConditions(allMembers);
        query.setDistinct(true);
        return query.fetchInto(PROJECT_MEMBER);
    }

    @Override
    public ProjectMember getProjectMember(long id) {
        return create()
                .selectFrom(PROJECT_MEMBER)
                .where(PROJECT_MEMBER.ID.eq(id)).fetchOne();
    }

    @Override
    public boolean hasAccessToProject(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identities) {
        return isProjectMember(projectId, usingAccount, isAdmin, identities);
    }

    @Override
    public boolean isProjectOwner(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identities) {
        if (identities == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        if (usingAccount != null && usingAccount.equals(projectId)) {
            return false;
        }
        Set<ProjectMemberRecord> projectMembers = new HashSet<>();
        Condition allMembers = DSL.falseCondition();
        for (Identity id : identities) {
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getExternalId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getExternalIdType()))
                    .and(PROJECT_MEMBER.ROLE.eq(ProjectConstants.OWNER))
                    .and(PROJECT_MEMBER.PROJECT_ID.eq(projectId))
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(PROJECT_MEMBER.REMOVED.isNull()));
        }
        projectMembers.addAll(create().selectFrom(PROJECT_MEMBER).where(allMembers).fetch());
        return !projectMembers.isEmpty();
    }

    @Override
    public boolean isProjectMember(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identities) {
        if (identities == null) {
            return false;
        }
        if ((usingAccount != null && usingAccount.equals(projectId)) || isAdmin) {
            return true;
        }
        Set<ProjectMemberRecord> projectMembers = new HashSet<>();
        Condition allMembers = DSL.falseCondition();
        for (Identity id : identities) {
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getExternalId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getExternalIdType()))
                    .and(PROJECT_MEMBER.PROJECT_ID.eq(projectId))
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(PROJECT_MEMBER.REMOVED.isNull()));
        }
        projectMembers.addAll(create().selectFrom(PROJECT_MEMBER).where(allMembers).fetch());
        return !projectMembers.isEmpty();
    }

    @Override
    public List<? extends ProjectMember> setProjectMembers(final Account project, final Set<Member> members,
                                                           final IdFormatter idFormatter) {
        return lockManager.lock(new ProjectLock(project), new LockCallback<List<? extends ProjectMember>>() {
            @Override
            public List<? extends ProjectMember> doWithLock() {
                List<? extends ProjectMember> previousMembers = getActiveProjectMembers(project.getId());
                Set<Member> otherPreviousMembers = new HashSet<>();
                for (ProjectMember member : previousMembers) {
                    String projectId = (String) idFormatter.formatId(objectManager.getType(Account.class), member.getProjectId());
                    otherPreviousMembers.add(new Member(member, projectId));
                }
                Set<Member> create = new HashSet<>(members);
                Set<Member> delete = new HashSet<>(otherPreviousMembers);
                for (Member member : members) {
                    if (delete.remove(member)) {
                        create.remove(member);
                    }
                }
                Condition allMembers = DSL.falseCondition();
                for (Member member : delete) {
                    allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(member.getExternalId())
                            .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(member.getExternalIdType()))
                            .and(PROJECT_MEMBER.PROJECT_ID.eq(project.getId()))
                            .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE)));
                }
                List<? extends ProjectMember> toDelete = create().selectFrom(PROJECT_MEMBER).where(allMembers).fetch();
                for (ProjectMember member : toDelete) {
                    objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE, member, null);
                    objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, member, null);
                }
                for (Member member : create) {
                    createProjectMember(project, member);
                }
                return getActiveProjectMembers(project.getId());
            }
        });
    }

    @Override
    public ProjectMember createProjectMember(Account project, Member member) {
        Map<Object, Object> properties = new HashMap<>();
        properties.put(PROJECT_MEMBER.PROJECT_ID, project.getId());
        properties.put(PROJECT_MEMBER.ACCOUNT_ID, project.getId());
        properties.put(PROJECT_MEMBER.NAME, member.getName());
        properties.put(PROJECT_MEMBER.EXTERNAL_ID, member.getExternalId());
        properties.put(PROJECT_MEMBER.EXTERNAL_ID_TYPE, member.getExternalIdType());
        properties.put(PROJECT_MEMBER.ROLE, member.getRole());
        return resourceDao.create(ProjectMember.class, objectManager.convertToPropertiesFor(ProjectMember.class, properties));
    }

    @Override
    public void ensureAllProjectsHaveNonRancherIdMembers(Identity identity) {
        //This operation is expensive if there are alot of projects and members however this is
        //only called when auth is being turned on. In most cases this will only be called once.
        Member newMember = new Member(identity, ProjectConstants.OWNER);
        Set<Identity> identities = new HashSet<>();
        Account account = getAdminAccount();
        identities.add(new Identity(ProjectConstants.RANCHER_ID, String.valueOf(account.getId())));
        List<Account> allProjects = getAccessibleProjects(identities, true, null);
        for (Account project : allProjects) {
            List<? extends ProjectMember> members = getActiveProjectMembers(project.getId());
            boolean hasNonRancherMember = false;
            for (ProjectMember member : members) {
                if (!member.getExternalIdType().equalsIgnoreCase(ProjectConstants.RANCHER_ID)) {
                    hasNonRancherMember = true;
                } else if (member.getExternalId().equals(String.valueOf(getAdminAccount().getId()))) {
                    deactivateThenRemove(member);
                } else {
                    hasNonRancherMember = true;
                }
            }
            if (!hasNonRancherMember) {
                createProjectMember(project, newMember);
            }
        }
    }

    private void deactivateThenRemove(ProjectMember member) {
        Object state = ObjectUtils.getPropertyIgnoreErrors(member, ObjectMetaDataManager.STATE_FIELD);

        if (CommonStatesConstants.ACTIVE.equals(state)) {
            objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE, member, null);
            member = objectManager.reload(member);
        }

        if (CommonStatesConstants.PURGED.equals(state)) {
            return;
        }

        objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, member, null);
    }

    @Override
    public boolean hasAccessToAnyProject(Set<Identity> identities, boolean isAdmin, Long usingAccount) {
        return !getAccessibleProjects(identities, isAdmin, usingAccount).isEmpty();
    }
}
