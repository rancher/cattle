package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SetProjectMembersActionHandler implements ActionHandler{

    @Inject
    AuthDao authDao;

    @Inject
    ProjectMemberResourceManager projectMemberResourceManager;

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

    @Override
    public String getName() {
        return "account.setmembers";
    }
}

