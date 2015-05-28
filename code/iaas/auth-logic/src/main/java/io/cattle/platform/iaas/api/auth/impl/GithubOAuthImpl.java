package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountAccess;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl implements AccountLookup, Priority {

    @Inject
    private AuthDao authDao;
    @Inject
    private GithubUtils githubUtils;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public AccountAccess getAccountAccess(ApiRequest request) {
        if (StringUtils.equals("token", request.getType())) {
            return null;
        }
        String token = request.getServletContext().getRequest().getHeader(ProjectConstants.AUTH_HEADER);
        if (StringUtils.isEmpty(token) || !token.toLowerCase().startsWith(ProjectConstants.AUTH_TYPE)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                return null;
            }
        }
        String projectId = request.getServletContext().getRequest().getHeader(ProjectConstants.PROJECT_HEADER);
        if (projectId == null || projectId.isEmpty()) {
            projectId = request.getServletContext().getRequest().getParameter("projectId");
        }
        return getAccountAccessInternal(token, projectId);
    }

    private AccountAccess getAccountAccessInternal(String token, String projectId){
        Account project = null;
        String accountId = githubUtils.validateAndFetchAccountIdFromToken(token);
        if (null == accountId) {
            return null;
        }
        Account account = authDao.getAccountByExternalId(accountId, GithubUtils.USER_SCOPE);
        if (account == null) {
            return null;
        }
        Set<ExternalId> externalIds = githubUtils.getExternalIds();
        if (externalIds != null) {
            externalIds.add(new ExternalId(String.valueOf(account.getId()), ProjectConstants.RANCHER_ID));
        }
        if (StringUtils.isEmpty(projectId)) {
            project = account;
        }
        if (project != null){
            return new AccountAccess(project, externalIds);
        }

        String unobfuscatedId;

        try {
            unobfuscatedId = ApiContext.getContext().getIdFormatter().parseId(projectId);
        } catch (NumberFormatException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "projectId header format is incorrect " + projectId, null);
        }

        if (StringUtils.isEmpty(unobfuscatedId)) {
            return null;
        }
        try{
            project = authDao.getAccountById(new Long(unobfuscatedId));
        } catch (NumberFormatException e){
            if (!unobfuscatedId.equalsIgnoreCase("user")){
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
            return new AccountAccess(account, externalIds);
        }
        if (project != null && authDao.hasAccessToProject(project.getId(), null,
                account.getKind().equalsIgnoreCase(AccountConstants.ADMIN_KIND), externalIds)) {
            return new AccountAccess(project, externalIds);
        }
        return null;
    }

    public AccountAccess getAccountAccess(String token, String projectId, ApiRequest request){
        request.getServletContext().getRequest().setAttribute(ProjectConstants.OAUTH_BASIC, token);
        return getAccountAccessInternal(token, projectId);
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }
}
