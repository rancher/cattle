package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubOAuthImpl implements AccountLookup, Priority {

    private static final String AUTH_HEADER = "Authorization";
    private static final String GITHUB_ACCOUNT_TYPE = "github";

    private AuthDao authDao;
    private TokenService tokenService;

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
        String accountId = validateAndFetchAccountId(token);
        return authDao.getAccountByExternalId(accountId, GITHUB_ACCOUNT_TYPE);

    }

    private String validateAndFetchAccountId(String token) {
        String toParse = null;
        String[] tokenArr = token.split("\\s+");
        if (tokenArr.length == 2) {
            if(!StringUtils.equals("bearer", StringUtils.lowerCase(StringUtils.trim(tokenArr[0])))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { //in case of invalid token
            return null;
        }
        return (String) jsonData.get("account_id");
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Inject
    public void setJwtTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }
}
