package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LocalAuthImpl extends LocalAuthConfigurable implements AccountLookup, Priority {
    @Inject
    LocalAuthUtils localAuthUtils;

    @Override
    public Account getAccount(ApiRequest request) {
        if (localAuthUtils.findAndSetJWT()){
            return localAuthUtils.getAccountFromJWT();
        }
        return null;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public String getName() {
        return LocalAuthConstants.AUTH_IMPL;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }
}
