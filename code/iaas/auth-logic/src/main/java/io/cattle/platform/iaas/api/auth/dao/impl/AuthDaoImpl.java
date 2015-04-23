package io.cattle.platform.iaas.api.auth.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;
import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.core.model.tables.records.ProjectMemberRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.api.auth.ProjectLock;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.Member;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.SelectQuery;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import com.netflix.config.DynamicStringListProperty;

public class AuthDaoImpl extends AbstractJooqDao implements AuthDao {

    private DynamicStringListProperty SUPPORTED_TYPES = ArchaiusUtil.getList("account.by.key.credential.types");

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    LockManager lockManager;

    @Override
    public Account getAdminAccount() {
        return create()
                .selectFrom(ACCOUNT)
                .where(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(ACCOUNT.KIND.eq("admin")))
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

    @Override
    public Account getAccountByExternalId(String externalId, String externalType) {
        return create()
                .selectFrom(ACCOUNT)
                .where(
                        ACCOUNT.EXTERNAL_ID.eq(externalId)
                                .and(ACCOUNT.EXTERNAL_ID_TYPE.eq(externalType))
                                .and(ACCOUNT.STATE.ne("purged"))
                ).orderBy(ACCOUNT.ID.asc()).limit(1).fetchOne();
    }

    @Override
    public Account createAccount(String name, String kind, String externalId, String externalType) {
        Map<Object, Object> properties = new HashMap<>();
        if(StringUtils.isNotEmpty(name)) {
            properties.put(ACCOUNT.NAME, name);
        }
        if(StringUtils.isNotEmpty(kind)) {
            properties.put(ACCOUNT.KIND, kind);
        }
        if(StringUtils.isNotEmpty(externalId)) {
            properties.put(ACCOUNT.EXTERNAL_ID, externalId);
        }
        if(StringUtils.isNotEmpty(externalType)) {
            properties.put(ACCOUNT.EXTERNAL_ID_TYPE, externalType);
        }
        return resourceDao.createAndSchedule(Account.class, objectManager.convertToPropertiesFor(Account.class, properties));
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
        return resourceDao.createAndSchedule(Account.class, objectManager.convertToPropertiesFor(Account.class, properties));
    }

    public Account createDefaultProject(Account account) {
        Account defaultProject = createProject(account.getName() + ProjectConstants.PROJECT_DEFAULT_NAME, null);
        objectManager.setFields(account, ACCOUNT.PROJECT_ID, defaultProject.getId());
        return defaultProject;
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
    public void updateAccount(Account account, String name, String kind, String externalId, String externalType) {
        Map<TableField<AccountRecord, String>, String> properties = new HashMap<>();
        if(StringUtils.isNotEmpty(name)) {
            properties.put(ACCOUNT.NAME, name);
        }
        if(StringUtils.isNotEmpty(kind)) {
            properties.put(ACCOUNT.KIND, kind);
        }
        if(StringUtils.isNotEmpty(externalId)) {
            properties.put(ACCOUNT.EXTERNAL_ID, externalId);
        }
        if(StringUtils.isNotEmpty(externalType)) {
            properties.put(ACCOUNT.EXTERNAL_ID_TYPE, externalType);
        }
        int updateCount = create()
                .update(ACCOUNT)
                .set(properties)
                .where(ACCOUNT.ID
                        .eq(account.getId()))
                        .execute();

        if(1 != updateCount) {
            throw new RuntimeException("UpdateAccount failed.");
        }
    }

    @Override
    public List<Account> getAccessibleProjects (Set<ExternalId> externalIds, boolean isAdmin, Long usingAccount) {
        List<Account> projects = new ArrayList<>();
        if(externalIds == null) {
            return projects;
        }
        if (isAdmin) {
            projects.addAll(create()
                    .selectFrom(ACCOUNT)
                    .where(ACCOUNT.KIND.eq(ProjectConstants.TYPE)
                            .and(ACCOUNT.STATE.eq(CommonStatesConstants.ACTIVE)))
                    .orderBy(ACCOUNT.ID.asc())
                    .fetch());
            return projects;
        }

        if (usingAccount != null){
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
        for (ExternalId id: externalIds){
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getType()))
                    .and(PROJECT_MEMBER.REMOVED.isNull())
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE)));
        }
        SelectQuery query = create().selectQuery();
        query.addFrom(ACCOUNT);
        query.addJoin(PROJECT_MEMBER, PROJECT_MEMBER.PROJECT_ID.equal(ACCOUNT.ID));
        query.addConditions(allMembers);
        query.setDistinct(true);
        projects.addAll(query.fetchInto(ACCOUNT));
        Map<Long, Account> returnProjects = new HashMap();
        for (Account project: projects){
            returnProjects.put(project.getId(), project);
        }
        projects = new ArrayList<>();
        projects.addAll(returnProjects.values());
        return projects;
    }

    public List<? extends ProjectMember> getActiveProjectMembers(long id) {
        return create()
                .selectFrom(PROJECT_MEMBER)
                .where(PROJECT_MEMBER.PROJECT_ID.eq(id))
                .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                .and(PROJECT_MEMBER.REMOVED.isNull())
                .orderBy(PROJECT_MEMBER.ID.asc()).fetch();
    }

    @Override
    public ProjectMember getProjectMember(long id) {
        return create()
                .selectFrom(PROJECT_MEMBER)
                .where(PROJECT_MEMBER.ID.eq(id)).fetchOne();
    }

    @Override
    public boolean hasAccessToProject (long projectId, Long usingAccount, boolean isAdmin, Set<ExternalId> externalIds) {
        return isProjectMember(projectId, usingAccount, isAdmin, externalIds);
    }

    @Override
    public boolean isProjectOwner (long projectId, Long usingAccount, boolean isAdmin, Set<ExternalId> externalIds){
        if (externalIds == null) {
            return false;
        }
        if (isAdmin) {
            return true;
        }
        if (usingAccount !=  null && usingAccount.equals(projectId)){
            return false;
        }
        Set<ProjectMemberRecord> projectMembers = new HashSet<>();
        Condition allMembers = DSL.falseCondition();
        for (ExternalId id: externalIds){
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getType()))
                    .and(PROJECT_MEMBER.ROLE.eq(ProjectConstants.OWNER))
                    .and(PROJECT_MEMBER.PROJECT_ID.eq(projectId))
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(PROJECT_MEMBER.REMOVED.isNull()));
        }
        projectMembers.addAll(create().selectFrom(PROJECT_MEMBER).where(allMembers).fetch());
        return !projectMembers.isEmpty();
    }
    
    public boolean isProjectMember (long projectId, Long usingAccount, boolean isAdmin,Set<ExternalId> externalIds){
        if (externalIds == null) {
            return false;
        }
        if ((usingAccount != null && usingAccount.equals(projectId)) || isAdmin) {
            return true;
        }
        Set<String> roles =  new HashSet<>();
        roles.add(ProjectConstants.OWNER);
        roles.add(ProjectConstants.MEMBER);
        Set<ProjectMemberRecord> projectMembers = new HashSet<>();
        Condition allMembers = DSL.falseCondition();
        for (ExternalId id: externalIds){
            allMembers = allMembers.or(PROJECT_MEMBER.EXTERNAL_ID.eq(id.getId())
                    .and(PROJECT_MEMBER.EXTERNAL_ID_TYPE.eq(id.getType()))
                    .and(PROJECT_MEMBER.ROLE.in(roles))
                    .and(PROJECT_MEMBER.PROJECT_ID.eq(projectId))
                    .and(PROJECT_MEMBER.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(PROJECT_MEMBER.REMOVED.isNull()));
        }
        projectMembers.addAll(create().selectFrom(PROJECT_MEMBER).where(allMembers).fetch());
        return !projectMembers.isEmpty();
    }

    @Override
    public Account updateProject(Account project, String name, String description) {
        Map<TableField<AccountRecord, String>, String> properties = new HashMap<>();
        if (name != null) {
            properties.put(ACCOUNT.NAME, name);
        }
        if (description != null) {
            properties.put(ACCOUNT.DESCRIPTION, description);
        }
        if (name == null && description == null){
            return project;
        }
        int updateCount = create()
                .update(ACCOUNT)
                .set(properties)
                .where(ACCOUNT.ID
                        .eq(project.getId()))
                .execute();
        if(1 != updateCount) {
            throw new RuntimeException("UpdateProject failed.");
        }
        return getAccountById(project.getId());
    }

    @Override
    public List<? extends ProjectMember> setProjectMembers(final Account project, final Set<Member> members) {
        return lockManager.lock(new ProjectLock(project), new LockCallback<List<? extends ProjectMember>>() {
            public List<? extends ProjectMember> doWithLock() {
                List<? extends ProjectMember> previousMembers = getActiveProjectMembers(project.getId());
                Set<Member> otherPreviosMembers = new HashSet<>();
                for (ProjectMember member : previousMembers) {
                    otherPreviosMembers.add(new Member(member));
                }
                HashSet<Member> create = (HashSet<Member>) ((HashSet<Member>) members).clone();
                HashSet<Member> delete = (HashSet<Member>) ((HashSet<Member>) otherPreviosMembers).clone();
                for (Member member : members) {
                    if (delete.remove(member)){
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
                    objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, member, null);
                }
                List<ProjectMember> newMembers = new ArrayList<>();
                for (Member member : create) {
                    newMembers.add(createProjectMember(project, member));
                }
                return getActiveProjectMembers(project.getId());
            }
        });
    }

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
    public Account setDefaultProject(Account project, long accountId) {
        if (project == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "Failed", "No project specified.", null);
        }
        Account account = getAccountById(accountId);
        if (accountId != project.getId() && project.getKind().equalsIgnoreCase(ProjectConstants.TYPE)
                && !account.getKind().equalsIgnoreCase(ProjectConstants.TYPE)) {
            int updateCount = create()
                    .update(ACCOUNT)
                    .set(ACCOUNT.PROJECT_ID, project.getId())
                    .where(ACCOUNT.ID
                            .eq(accountId))
                    .execute();

            if (1 != updateCount) {
                throw new ClientVisibleException(ResponseCodes.NOT_MODIFIED, "Failed", "Failed to update account default project.", null);
            }
            return project;
        } else {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "Failed", "Cannot Use your account as a default project", null);

        }
    }

    @Override
    public Account getDefaultProject(Account account) {
        Account project = getAccountById(account.getProjectId());
        if (project == null){
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "DefaultProjectNotFound", "Current Default Project Not Found. Please Select Another.", null);
        }
        return project;
    }

    @Override
    public boolean hasAccessToAnyProject(Set<ExternalId> externalIds, boolean isAdmin, Long usingAccount) {
        return !getAccessibleProjects(externalIds, isAdmin, usingAccount).isEmpty();
    }
}
