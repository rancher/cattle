package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class GithubOAuthImpl implements AccountLookup, Priority {

    private static final String AUTH_HEADER = "Authorization";
    private static final String X_GITHUB_CLIENT_ID = "X-GitHub-Client-Id";
    private static final String GITHUB_ACCOUNT_TYPE = "github";

    private AuthDao authDao;
    private TokenService tokenService;

    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        String token = request.getServletContext().getRequest().getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try {
            String accountId = validateAndFetchAccountId(token, request.getServletContext().getResponse());
            return authDao.getAccountByExternalId(accountId, GITHUB_ACCOUNT_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    private String validateAndFetchAccountId(String token, HttpServletResponse response) {
        try {
            token = token.split("\\s+")[1];
            Map<String, Object> jsonData = tokenService.getJsonPayload(token, true);
            return (String) jsonData.get("account_id");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean challenge(ApiRequest request) {
        HttpServletResponse response = request.getServletContext().getResponse();
        response.setHeader(X_GITHUB_CLIENT_ID, GITHUB_CLIENT_ID.get());
        return true;
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
