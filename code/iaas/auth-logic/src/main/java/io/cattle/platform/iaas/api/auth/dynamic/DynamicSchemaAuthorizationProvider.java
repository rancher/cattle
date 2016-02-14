package io.cattle.platform.iaas.api.auth.dynamic;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Set;

import javax.inject.Inject;

public class DynamicSchemaAuthorizationProvider implements AuthorizationProvider {

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Inject
    JsonMapper jsonMapper;

    AuthorizationProvider authorizationProvider;

    @Inject
    AuthDao authDao;

    @Override
    public SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request) {
        SchemaFactory factory = authorizationProvider.getSchemaFactory(account, policy, request);

        if (factory == null) {
            return null;
        }

        return new DynamicSchemaFactory(account.getId(), factory, dynamicSchemaDao, jsonMapper,
                getRole(account, policy, request));
    }

    @Override
    public String getRole(Account account, Policy policy, ApiRequest request) {
        return authorizationProvider.getRole(account, policy, request);
    }

    @Override
    public Policy getPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        return authorizationProvider.getPolicy(account, authenticatedAsAccount, identities, request);
    }

    public AuthorizationProvider getAuthorizationProvider() {
        return authorizationProvider;
    }

    public void setAuthorizationProvider(AuthorizationProvider authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

}
