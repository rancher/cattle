package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountAccess;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;

public class AdminAuthLookUp implements AccountLookup, Priority {

    private static final String ENFORCE_AUTH_HEADER = "X-ENFORCE-AUTHENTICATION";
    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");

    @Inject
    AuthDao authDao;

    @Override
    public AccountAccess getAccountAccess(ApiRequest request) {
        AccountAccess accountAccess = null;
        if (SECURITY.get()){
            return accountAccess;
        }

        String authHeader = StringUtils.trim(request.getServletContext().getRequest().getHeader(ENFORCE_AUTH_HEADER));
        if (StringUtils.equals("true", authHeader)) {
            return accountAccess;
        }
        Account admin = authDao.getAdminAccount();
        if (admin == null){
            return null;
        }
        String projectId = request.getServletContext().getRequest().getHeader(ProjectConstants.PROJECT_HEADER);
        Account project = null;
        if (projectId == null || projectId.isEmpty()) {
            projectId = request.getServletContext().getRequest().getParameter("projectId");
        }
        if (projectId != null && !projectId.isEmpty() && !projectId.equalsIgnoreCase(ProjectConstants.USER)) {
            String id = ApiContext.getContext().getIdFormatter().parseId(projectId);
            project = authDao.getAccountById(Long.valueOf(id));
            if (project == null){
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
        }
        if (project == null) {
            project = authDao.getDefaultProject(admin);
        }
        if (projectId != null && projectId.equalsIgnoreCase(ProjectConstants.USER)) {
            accountAccess = new AccountAccess(admin, null);
            accountAccess.getExternalIds().add(new ExternalId(String.valueOf(admin.getId()), ProjectConstants.RANCHER_ID));
        }else{
            accountAccess = new AccountAccess(project, null);
            accountAccess.getExternalIds().add(new ExternalId(String.valueOf(project.getId()), ProjectConstants.RANCHER_ID));
        }
        return accountAccess;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
