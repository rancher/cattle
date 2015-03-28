package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

public class GithubOAuthImpl implements AccountLookup, Priority {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_TYPE = "bearer ";
    private static final String GITHUB_ACCOUNT_TYPE = "github";
    private static final String PROJECT_HEADER = "X-API-Project-Id";
    private static final String USER_SCOPE = "project:github_user";
    private static final String ORG_SCOPE = "project:github_org";
    private static final String TEAM_SCOPE = "project:github_team";

    private AuthDao authDao;
    private GithubUtils githubUtils;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        if (StringUtils.equals("token", request.getType())) {
            return null;
        }
        String token = request.getServletContext().getRequest().getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token) || !token.toLowerCase().startsWith(AUTH_TYPE)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                return null;
            }
        }
        String accountId = githubUtils.validateAndFetchAccountIdFromToken(token);
        if (null == accountId) {
            return null;
        }
        Account account = authDao.getAccountByExternalId(accountId, GITHUB_ACCOUNT_TYPE);

        String projectId = request.getServletContext().getRequest().getHeader(PROJECT_HEADER);
        if (StringUtils.isEmpty(projectId)) {
            projectId = request.getServletContext().getRequest().getParameter("projectId");
            if (StringUtils.isEmpty(projectId)) {
                return account;
            }
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

        Account projectAccount = authDao.getAccountById(new Long(unobfuscatedId));

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
        List<String> accesibleIds = null;
        if (StringUtils.equals(TEAM_SCOPE, account.getExternalIdType())) {
            accesibleIds = githubUtils.validateAndFetchTeamIdsFromToken(token);
        } else if (StringUtils.equals(ORG_SCOPE, account.getExternalIdType())) {
            accesibleIds = githubUtils.validateAndFetchOrgIdsFromToken(token);
        }
        if (StringUtils.equals(USER_SCOPE, projectAccount.getExternalIdType())) {
            account = authDao.getAccountByExternalId(projectAccount.getExternalId(), GITHUB_ACCOUNT_TYPE);

            accesibleIds = ImmutableList.of(githubUtils.validateAndFetchAccountIdFromToken(token));
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
