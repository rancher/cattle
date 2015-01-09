package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TokenAccountLookup implements AccountLookup, Priority {

    private AuthDao authDao;
    private static final String TOKEN = "token";

    @Override
    public Account getAccount(ApiRequest request) {
        Account account = null;
        if (StringUtils.equals("token", request.getType())) {
            if (!Method.POST.isMethod(request.getMethod()) && !Method.GET.isMethod(request.getMethod())) {
                throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, "only POST/GET is allowed for /v1/token API");
            }
            account = authDao.getAccountByUuid(TOKEN);
        }
        return account;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

}
