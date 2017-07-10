package io.cattle.platform.iaas.api.auth.integration.register;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.impl.DefaultAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.BasicAuthImpl;
import io.cattle.platform.register.auth.RegistrationAuthTokenManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class RegistrationTokenAccountLookup implements AccountLookup {

    RegistrationAuthTokenManager tokenManager;

    public RegistrationTokenAccountLookup(RegistrationAuthTokenManager tokenManager) {
        super();
        this.tokenManager = tokenManager;
    }

    @Override
    public Account getAccount(ApiRequest request) {
        String[] auth = BasicAuthImpl.getUsernamePassword(request);

        if (auth == null) {
            return null;
        }

        String username = auth[0];
        String password = auth[1];

        if (!CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN.equals(username)) {
            return null;
        }

        Account account = tokenManager.validateToken(password);

        if (account == null) {
            return null;
        }

        request.setAttribute(DefaultAuthorizationProvider.ACCOUNT_SCHEMA_FACTORY_NAME, RegisterConstants.SCHEMA_NAME);

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
        return "RegistrationToken";
    }
}
