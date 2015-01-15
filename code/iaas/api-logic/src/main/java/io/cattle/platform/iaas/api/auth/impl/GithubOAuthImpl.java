package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl implements AccountLookup, Priority {

    private static final String AUTH_HEADER = "Authorization";
    private static final String GITHUB_ACCOUNT_TYPE = "github";
    private static final String PROJECT_HEADER = "X-API-Project-Id";
    private static final String USER_SCOPE = "project:github_user";

    private AuthDao authDao;
    private GithubUtils githubUtils;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        String token = request.getServletContext().getRequest().getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                return null;
            }
        }
        String accountId = githubUtils.validateAndFetchAccountIdFromToken(token);
        if(null == accountId) {
            return null;
        }
        Account account = authDao.getAccountByExternalId(accountId, GITHUB_ACCOUNT_TYPE);

        String projectId = request.getServletContext().getRequest().getHeader(PROJECT_HEADER);
        if (StringUtils.isEmpty(projectId)) {
            return account;
        }

        Account projectAccount = authDao.getAccountById(new Long(projectId));

        if (validateProjectAccount(projectAccount, token)) {
            return projectAccount;
        }
        return null;
    }

    protected boolean validateProjectAccount(Account projectAccount, String token) {
        if (null == projectAccount) {
            return false;
        }
        Account account = projectAccount;
        List<String> accesibleIds = githubUtils.validateAndFetchAccesibleIdsFromToken(token);
        if (StringUtils.equals(USER_SCOPE, projectAccount.getExternalIdType())) {
            account = authDao.getAccountById(new Long(projectAccount.getExternalId()));
        }

        return accesibleIds.contains(account.getExternalId());
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setGithubUtils(GithubUtils githubUtils) {
        this.githubUtils = githubUtils;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }
}
