package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SetProjectMembersActionHandler implements ActionHandler {

    AuthDao authDao;
    ProjectMemberResourceManager projectMemberResourceManager;

    public SetProjectMembersActionHandler(AuthDao authDao, ProjectMemberResourceManager projectMemberResourceManager) {
        this.authDao = authDao;
        this.projectMemberResourceManager = projectMemberResourceManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Account project = (Account) obj;
        project = authDao.getAccountById(project.getId());
        if (project == null) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        LinkedHashMap<String, Object> reqObj = (LinkedHashMap<String, Object>) request.getRequestObject();
        List<Map<String, String>> members = (List<Map<String, String>>) reqObj.get("members");
        return projectMemberResourceManager.setMembers(project, members);
    }

}

