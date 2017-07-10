package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import org.apache.commons.lang3.StringUtils;

public class TokenAccountLookup implements AccountLookup {

    private AuthDao authDao;

    public TokenAccountLookup(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        Account account = null;
        if (StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType())) {
            account = authDao.getAccountByUuid("token");
        }
        return account;
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
        return "TokenAccount";
    }
}
