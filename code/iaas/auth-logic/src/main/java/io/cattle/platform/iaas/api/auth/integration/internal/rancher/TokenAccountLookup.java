package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TokenAccountLookup implements AccountLookup, Priority {

    @Inject
    private AuthDao authDao;

    @Override
    public Account getAccount(ApiRequest request) {
        Account account = null;
        if (StringUtils.equals(TokenUtils.TOKEN, request.getType())) {
            account = authDao.getAccountByUuid("token");
        }
        return account;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getName() {
        return "TokenAccountLookUp";
    }
}
