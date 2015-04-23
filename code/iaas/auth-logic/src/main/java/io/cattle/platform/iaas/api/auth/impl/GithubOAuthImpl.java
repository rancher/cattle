package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl implements AccountLookup, Priority {


    private static final String GITHUB_ACCOUNT_TYPE = "github";

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
        return getAccountAccessInternal(token, projectId, request);
    }

    private AccountAccess getAccountAccessInternal(String token, String projectId, ApiRequest request){
        Account project = null;
        String accountId = githubUtils.validateAndFetchAccountIdFromToken(token);
        if (null == accountId) {
            return null;
        }
        Account account = authDao.getAccountByExternalId(accountId, GITHUB_ACCOUNT_TYPE);
        Set<ExternalId> externalIds = githubUtils.getExternalIds();
        if (account != null && externalIds != null) {
            externalIds.add(new ExternalId(String.valueOf(account.getId()), ProjectConstants.RANCHER_ID));
        }
        if (StringUtils.isEmpty(projectId)) {
            if (account != null) {
                if (account.getProjectId() != null) {
                    project = authDao.getAccountById(account.getProjectId());
                    if (project == null) {
                        List<Account> projects = authDao.getAccessibleProjects(externalIds, false, null);
                        if (projects.isEmpty()) {
                            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "NoProject", "You don't have access to any projects.", null);
                        } else {
                            project = projects.get(0);
                        }
                    }
                }
            }
        } else if (projectId.equalsIgnoreCase(ProjectConstants.USER)){
            project = account;
        }
        if (project != null){
            return new AccountAccess(project, externalIds);
        }

        String unobfuscatedId = null;

        try {
            unobfuscatedId = ApiContext.getContext().getIdFormatter().parseId(projectId);
        } catch (NumberFormatException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "projectId header format is incorrect " + projectId, null);
        }

        if (StringUtils.isEmpty(unobfuscatedId)) {
            return null;
        }

        project = authDao.getAccountById(new Long(unobfuscatedId));
        if (project != null && authDao.hasAccessToProject(project.getId(), null, account.getKind().equalsIgnoreCase("admin"), externalIds)) {
            return new AccountAccess(project, externalIds);
        }
        return null;
    }

    public AccountAccess getAccountAccess(String token, String projectId, ApiRequest request){
        request.getServletContext().getRequest().setAttribute(ProjectConstants.OAUTH_BASIC, token);
        return getAccountAccessInternal(token, projectId, request);
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }
}
