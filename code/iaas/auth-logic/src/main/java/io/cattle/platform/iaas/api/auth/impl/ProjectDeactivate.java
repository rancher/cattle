package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

public class ProjectDeactivate implements ActionHandler{

    @Inject
    AuthDao authDao;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ObjectManager objectManager;

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Policy policy = (Policy) ApiContext.getContext().getPolicy();
        Account project = (Account) obj;
        if (!project.getKind().equalsIgnoreCase(ProjectConstants.TYPE)){
            return null;
        }
        project = authDao.getAccountById(project.getId());
        if (project == null) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }
        objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, project, null);
        project = objectManager.reload(project);
        return project;
    }

    @Override
    public String getName() {
        return "account.deactivate";
    }
}

