package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractObjectResourceManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.Member;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

public class ProjectResourceManager extends AbstractObjectResourceManager {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    AuthDao authDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ProjectMemberResourceManager projectMemberResourceManager;

    @Override
    public String[] getTypes() {
        return new String[] { "project" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

    @Override
    public Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Object obj = criteria.get("id");
        String id;
        if (obj != null) {
            if (obj instanceof String) {
                id = (String) obj;
            }else {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
            Account project = giveProjectAccess(getObjectManager().loadResource(Account.class, id), policy);
            return Collections.singletonList(project);
        }
        if (criteria.get("uuid") != null) {
            String uuid = ((String) ((Condition) ((ArrayList) criteria.get("uuid")).get(0)).getValue());
            return giveProjectAccess(authDao.getAccountByUuid(uuid), policy);
        }
        boolean isAdmin;
        Object getAll = request.getRequestParams().get("all");
        if (getAll != null){
            String all = ((String[]) getAll)[0];
            if (all.equalsIgnoreCase("true") && policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS)){
                isAdmin = true;
            }else {
                isAdmin = false;
            }
        } else{
            isAdmin = false;
        }
        List<Account> projects = authDao.getAccessibleProjects(policy.getExternalIds(),
            isAdmin, policy.getAccountId());
        for (Account project: projects){
            giveProjectAccess(project, policy);
        }
        return  projects;
    }

    private Account giveProjectAccess(Account project, Policy policy) {
        if (project == null) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (!authDao.hasAccessToProject(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (!project.getState().equals(CommonStatesConstants.ACTIVE) && !authDao.isProjectOwner(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())){
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (authDao.isProjectOwner(project.getId(), policy.getAccountId(), policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())) {
            DataAccessor.fields(project).withKey(ObjectMetaDataManager.CAPABILITIES_FIELD).set(Arrays.asList("owner"));
        }
        policy.grantObjectAccess(project);
        return project;
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!ProjectConstants.TYPE.equals(type)) {
            return null;
        }
        return createProject(request);
    }

    private Account createProject(ApiRequest apiRequest) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Map<String, Object> project = CollectionUtils.toMap(apiRequest.getRequestObject());
        if (authDao.getAccountById(policy.getAccountId()).getKind().equalsIgnoreCase(ProjectConstants.TYPE)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        Account newProject = authDao.createProject((String) project.get("name"), (String) project.get("description"));
        List<Map<String, String>> members = (ArrayList<Map<String, String>>) project.get("members");
        projectMemberResourceManager.setMembers(newProject, members);
        policy.grantObjectAccess(newProject);

        return newProject;
    }

    public Account createProjectForUser(Account account) {
        Account project = authDao.createProject(account.getName() + ProjectConstants.PROJECT_DEFAULT_NAME, null);
        ExternalId externalId = new ExternalId(account.getExternalId(), account.getExternalIdType(), account.getName());
        authDao.createProjectMember(project, new Member(externalId, ProjectConstants.OWNER));

        return project;
    }

    @Override
    protected Object deleteInternal(String type, String id, final Object obj, ApiRequest apiRequest) {
        if (!(obj instanceof Account) || !(((Account) obj).getKind().equalsIgnoreCase(ProjectConstants.TYPE))) {
            return super.deleteInternal(type, id, obj, apiRequest);
        }
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        if (authDao.getAccountById(Long.valueOf(id)) == null){
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (!authDao.isProjectOwner(Long.valueOf(id), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        try {
            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, obj, null);
        } catch (ProcessCancelException e) {
            objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE, obj,
                    ProcessUtils.chainInData(new HashMap<String, Object>(), AccountConstants.ACCOUNT_DEACTIVATE, AccountConstants.ACCOUNT_REMOVE));
        }
        Account deletedProject = (Account) objectManager.reload(obj);
        for (ProjectMember member: authDao.getActiveProjectMembers(deletedProject.getId())){
            objectProcessManager.executeStandardProcess(StandardProcess.DEACTIVATE, member, null);
            objectProcessManager.executeStandardProcess(StandardProcess.REMOVE, member, null);
        }
        policy.grantObjectAccess(deletedProject);
        return Arrays.asList(deletedProject);
    }

    @Override
    protected Object removeFromStore(String type, String id, Object obj, ApiRequest apiRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest apiRequest) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Account project = (Account) obj;
        if (authDao.isProjectOwner(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())) {
            return super.updateInternal(type, id, obj, apiRequest);
        }else {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "Forbidden", "You must be a project owner to update the name or description.", null);
        }
    }

    @Override
    protected Relationship getRelationship(String type, String linkName) {
        if (linkName.equalsIgnoreCase("projectmembers")) {
            Relationship rel = super.getMetaDataManager().getRelationship(type, linkName, "projectid");
            if (rel != null){
                return rel;
            }
        }
        return super.getRelationship(type, linkName);
    }

    @Override
    protected void addLinks(Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        super.addLinks(obj, schemaFactory, schema, resource);

        Map<String, URL> links = new TreeMap<>();
        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        for ( Schema childSchema : schemaFactory.listSchemas() ) {
            if ( ! schema.getCollectionMethods().contains(Schema.Method.GET.toString()) ) {
                continue;
            }

            URL link = urlBuilder.resourceLink(resource, childSchema.getPluralName());
            if ( link != null ) {
                links.put(childSchema.getPluralName(), link);
            }
        }

        resource.getLinks().putAll(links);
    }
}
