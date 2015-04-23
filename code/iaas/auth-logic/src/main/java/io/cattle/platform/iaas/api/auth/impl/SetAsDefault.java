package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

public class SetAsDefault implements ActionHandler{

    @Inject
    AuthDao authDao;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Account project = (Account) obj;
        project = authDao.getAccountById(project.getId());
        if (project == null || !project.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        if (!authDao.isProjectMember(project.getId(), policy.getAccountId(),
                policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), policy.getExternalIds())){
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "CantMakeDefault", "You must be" +
                    " a project member use a project as your default project.", null);
        }
        authDao.setDefaultProject(project, policy.getAccountId());
        request.setResponseCode(ResponseCodes.NO_CONTENT);
        request.commit();
        return null;

    }

    @Override
    public String getName() {
        return "account.setasdefault";
    }
}

