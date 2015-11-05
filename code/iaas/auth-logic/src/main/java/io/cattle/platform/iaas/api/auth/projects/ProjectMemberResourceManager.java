package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractObjectResourceManager;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.iaas.api.auth.identity.IdentityManager;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ProjectMemberResourceManager extends AbstractObjectResourceManager {

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
    IdentityManager identityManager;

    @Override
    protected Object removeFromStore(String type, String id, Object obj, ApiRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        String id = RequestUtils.makeSingularStringIfCan(criteria.get("id"));
        if (StringUtils.isNotEmpty(id)) {
            ProjectMember projectMember;
            try {
                 projectMember = authDao.getProjectMember(Long.valueOf(id));
            } catch (NumberFormatException e) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
            if (projectMember == null) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
            if (!authDao.hasAccessToProject(projectMember.getProjectId(), policy.getAccountId(),
                    policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getIdentities())) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
            Identity identity = identityManager.projectMemberToIdentity(projectMember);
            policy.grantObjectAccess(identity);
            return Collections.singletonList(identity);

        }
        String projectId = RequestUtils.makeSingularStringIfCan(criteria.get("projectId"));
        List<? extends ProjectMember> members;
        if (StringUtils.isNotEmpty(projectId)) {
            members = authDao.getActiveProjectMembers(Long.valueOf(projectId));
        } else {
            members = authDao.getActiveProjectMembers(policy.getAccountId());
        }
        List<Identity> identities = new ArrayList<>();
        for (ProjectMember member:members){
            Identity identity = identityManager.projectMemberToIdentity(member);
            identities.add(identity);
            policy.grantObjectAccess(identity);
        }
        return identities;
    }

    @Override
    public String[] getTypes() {
        return new String[]{"projectMember"};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
    }

    public List<ProjectMember> setMembers(Account project, List<Map<String, String>> members) {
        List<ProjectMember> membersCreated = new ArrayList<>();
        Set<Member> membersTransformed = new HashSet<>();

        if ((members == null || members.isEmpty())) {
            Policy policy = (Policy) ApiContext.getContext().getPolicy();
            Identity idToUse = null;
            for (Identity identity : policy.getIdentities()) {
                if (idToUse == null) {
                    if (identity.getExternalIdType().equalsIgnoreCase(ProjectConstants.RANCHER_ID)) {
                        idToUse = identity;
                    }
                }
            }
            if (idToUse != null) {
                Member owner = new Member(idToUse, ProjectConstants.OWNER);
                membersTransformed.add(owner);
            }

        } else {
            for (Map<String, String> newMember : members) {
                if (newMember.get("externalId") == null || newMember.get("externalIdType") == null || newMember.get("role") == null) {

                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "Member format " +
                            "invalid", null);
                }
            }

            for (Map<String, String> newMember : members) {
                Identity givenIdentity = new Identity(newMember.get("externalIdType"), newMember.get("externalId"));
                givenIdentity = identityManager.projectMemberToIdentity(givenIdentity);
                membersTransformed.add(new Member(givenIdentity, newMember.get("role")));
            }
        }


        boolean hasOwner = false;
        for (Member member : membersTransformed) {
            if (member.getRole().equalsIgnoreCase(ProjectConstants.OWNER)) {
                hasOwner = true;
            }
        }

        if (!hasOwner) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "Members list does not have an owner.", null);

        }
        membersCreated.addAll(authDao.setProjectMembers(project, membersTransformed, ApiContext.getContext()
                .getIdFormatter()));

        for (ProjectMember member : membersCreated) {
            identityManager.untransform(identityManager.projectMemberToIdentity(member), true);
        }
        return membersCreated;
    }
}
