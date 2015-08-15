package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl extends GithubConfigurable implements AccountLookup, Priority {

    @Inject
    private GithubUtils githubUtils;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        if (StringUtils.equals(TokenUtils.TOKEN, request.getType())) {
            return null;
        }
        githubUtils.findAndSetJWT();
        return getAccountAccessInternal();
    }

    private Account getAccountAccessInternal() {
        return githubUtils.getAccountFromJWT();
    }

    public Account getAccountAccess(String token, ApiRequest request) {
        request.setAttribute(GithubConstants.GITHUB_JWT, token);
        return getAccountAccessInternal();
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public String getName() {
        return GithubConstants.ACCOUNT_LOOKUP;
    }
}
