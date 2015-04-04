package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountAccess;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TokenAccountLookup implements AccountLookup, Priority {

    private AuthDao authDao;
    private static final String TOKEN = "token";

    @Override
    public AccountAccess getAccount(ApiRequest request) {
        Account account = null;
        if (StringUtils.equals("token", request.getType())) {
            account = authDao.getAccountByUuid(TOKEN);
        }
        return new AccountAccess(account, null);
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
        return Priority.PRE;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

}
