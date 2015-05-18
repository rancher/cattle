package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

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
    public Account getAccount(ApiRequest request) {
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
        if (!StringUtils.isEmpty(token) && token.toLowerCase().startsWith(ProjectConstants.AUTH_TYPE)){
            request.setAttribute(GithubConstants.GITHUB_JWT, token);
        }

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
