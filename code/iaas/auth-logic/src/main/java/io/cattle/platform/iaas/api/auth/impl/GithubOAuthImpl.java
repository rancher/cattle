package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl implements AccountLookup, Priority {

    @Inject
    private GithubUtils githubUtils;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        if (StringUtils.equals(GithubConstants.TOKEN, request.getType())) {
            return null;
        }
        githubUtils.findAndSetJWT();
        return getAccountAccessInternal();
    }

    private Account getAccountAccessInternal(){
        return githubUtils.getAccountFromJWT();
    }

    public Account getAccountAccess(String token, ApiRequest request){
        request.setAttribute(GithubConstants.GITHUB_JWT, token);
        return getAccountAccessInternal();
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }
}
