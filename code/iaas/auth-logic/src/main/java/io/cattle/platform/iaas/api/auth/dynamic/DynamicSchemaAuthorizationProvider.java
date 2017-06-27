package io.cattle.platform.iaas.api.auth.dynamic;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class DynamicSchemaAuthorizationProvider implements AuthorizationProvider {

    DynamicSchemaDao dynamicSchemaDao;
    JsonMapper jsonMapper;
    AuthorizationProvider authorizationProvider;

    Cache<String, Schema> schemaCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    public DynamicSchemaAuthorizationProvider(DynamicSchemaDao dynamicSchemaDao, JsonMapper jsonMapper, AuthorizationProvider authorizationProvider) {
        super();
        this.dynamicSchemaDao = dynamicSchemaDao;
        this.jsonMapper = jsonMapper;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request) {
        SchemaFactory factory = authorizationProvider.getSchemaFactory(account, policy, request);

        if (factory == null) {
            return null;
        }

        return new DynamicSchemaFactory(account.getId(), factory, dynamicSchemaDao, jsonMapper,
                getRole(account, policy, request), schemaCache);
    }

    @Override
    public String getRole(Account account, Policy policy, ApiRequest request) {
        return authorizationProvider.getRole(account, policy, request);
    }

    @Override
    public Policy getPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        return authorizationProvider.getPolicy(account, authenticatedAsAccount, identities, request);
    }

}
