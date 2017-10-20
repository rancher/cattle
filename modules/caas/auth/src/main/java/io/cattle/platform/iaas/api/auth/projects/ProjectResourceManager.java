package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectResourceManager extends DefaultResourceManager {

    AuthDao authDao;
    ProjectMemberResourceManager projectMemberResourceManager;
    AccountDao accountDao;

    public ProjectResourceManager(DefaultResourceManagerSupport support, AuthDao authDao, ProjectMemberResourceManager projectMemberResourceManager,
            AccountDao accountDao) {
        super(support);
        this.authDao = authDao;
        this.projectMemberResourceManager = projectMemberResourceManager;
        this.accountDao = accountDao;
    }

    @Override
    public Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        String id = RequestUtils.getConditionValue("id", criteria);
        String uuid = RequestUtils.getConditionValue("uuid", criteria);
        String name = RequestUtils.getConditionValue("name", criteria);
        if (!StringUtils.isBlank(id)) {
            Account project = giveProjectAccess(objectResourceManagerSupport.getObjectManager().loadResource(Account.class, id), policy);
            return Collections.singletonList(project);
        }
        if (!StringUtils.isBlank(uuid)) {
            return giveProjectAccess(authDao.getAccountByUuid(uuid), policy);
        }
        boolean isAdmin;
        Object getAll = request.getRequestParams().get("all");
        if (getAll != null) {
            String all = ((String[]) getAll)[0];
            isAdmin = all.equalsIgnoreCase("true") && policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS);
        } else {
            isAdmin = false;
        }
        List<Account> projects = authDao.getAccessibleProjects(policy.getIdentities(),
                isAdmin, policy.getAccountId());
        List<Account> projectsFiltered = new ArrayList<>();
        for (Account project : projects) {
            if (StringUtils.isNotBlank(name) && !name.equalsIgnoreCase(project.getName())) {
                continue;
            }
            projectsFiltered.add(giveProjectAccess(project, policy));
        }
        Object projectsByCriteria = super.listSupport(schemaFactory, type, criteria, options);
        if (projectsByCriteria != null) {
            projectsFiltered.retainAll((List<Account>) projectsByCriteria);
        }
        return projectsFiltered;
    }

    private Account giveProjectAccess(Account project, Policy policy) {
        if (project == null || !ProjectConstants.TYPE.equalsIgnoreCase(project.getKind())) {
            return null;
        }
        if (!authDao.hasAccessToProject(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getIdentities())) {
            return null;
        }
        boolean isOwner = authDao.isProjectOwner(project.getId(), policy.getAccountId(), policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy
                .getIdentities());
        if (!accountDao.isActiveAccount(project) && !isOwner) {
            return null;
        }
        if (isOwner) {
            ApiContext.getContext().addCapability(project, ProjectConstants.OWNER);
        } else {
            ApiContext.getContext().setCapabilities(project, new ArrayList<>());
        }
        policy.grantObjectAccess(project);
        return project;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        if (!ProjectConstants.TYPE.equals(type)) {
            return null;
        }
        return createProject(type, request);
    }

    @SuppressWarnings("unchecked")
    private Account createProject(String type, ApiRequest apiRequest) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Map<String, Object> project = CollectionUtils.toMap(apiRequest.getRequestObject());
        if (authDao.getAccountById(policy.getAccountId()).getKind().equalsIgnoreCase(ProjectConstants.TYPE)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        Object object = super.create(type, apiRequest);
        if (object instanceof Account) {
            Account newProject = (Account) object;
            newProject.setKind(AccountConstants.PROJECT_KIND);
            objectResourceManagerSupport.getObjectManager().persist(newProject);
            List<Map<String, String>> members = (ArrayList<Map<String, String>>) project.get("members");
            projectMemberResourceManager.setMembers(newProject, members);
            policy.grantObjectAccess(newProject);
            return objectResourceManagerSupport.getObjectManager().reload(newProject);
        } else {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR);
        }
    }

    public Account createProjectForUser(Identity identity) {
        Account project = authDao.createProject(identity.getLogin() + ProjectConstants.PROJECT_DEFAULT_NAME, null);
        authDao.createProjectMember(project, new Member(identity, ProjectConstants.OWNER));
        return project;
    }

    @Override
    public Object deleteObjectSupport(String type, String id, final Object obj, ApiRequest apiRequest) {
        if (!(obj instanceof Account) || !(((Account) obj).getKind().equalsIgnoreCase(ProjectConstants.TYPE))) {
            return super.deleteObjectSupport(type, id, obj, apiRequest);
        }
        ObjectProcessManager objectProcessManager = objectResourceManagerSupport.getObjectProcessManager();
        ObjectManager objectManager = objectResourceManagerSupport.getObjectManager();
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        if (authDao.getAccountById(Long.valueOf(id)) == null) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (!authDao.isProjectOwner(Long.valueOf(id), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getIdentities())) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        try {
            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, obj, null);
        } catch (ProcessCancelException e) {
            objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE, obj,
                    ProcessUtils.chainInData(new HashMap<>(), AccountConstants.ACCOUNT_DEACTIVATE, AccountConstants.ACCOUNT_REMOVE));
        }
        Account deletedProject = (Account) objectManager.reload(obj);
        for (ProjectMember member : authDao.getActiveProjectMembers(deletedProject.getId())) {
            objectManager.delete(member);
        }
        policy.grantObjectAccess(deletedProject);
        return Arrays.asList(deletedProject);
    }

    @Override
    public Object updateObjectSupport(String type, String id, Object obj, ApiRequest apiRequest) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Account project = (Account) obj;
        if (authDao.isProjectOwner(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getIdentities())) {
            return super.updateObjectSupport(type, id, obj, apiRequest);
        } else {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "Forbidden", "You must be a project owner to update the name or description.", null);
        }
    }

}
