package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountAccess;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class ApiAuthenticator extends AbstractApiRequestHandler {

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final Logger log = LoggerFactory.getLogger(ApiAuthenticator.class);

    private static final String ACCOUNT_ID_HEADER = "X-API-ACCOUNT-ID";

    AuthDao authDao;
    List<AccountLookup> accountLookups;
    List<AuthorizationProvider> authorizationProviders;
    @Inject
    ObjectManager objectManager;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (ApiContext.getContext().getPolicy() != null) {
            return;
        }

        AccountAccess accountAccess = getAccountAccess(request);
        if (accountAccess == null) {
            throwUnauthorizated();
        }

        Policy policy = getPolicy(accountAccess, request);
        if (policy == null) {
            log.error("Failed to find policy for [{}]", accountAccess.getAccount().getId());
            throwUnauthorizated();
        }

        SchemaFactory schemaFactory = getSchemaFactory(accountAccess.getAccount(), policy, request);
        if (schemaFactory == null) {
            log.error("Failed to find a schema for account type [{}]", accountAccess.getAccount().getKind());
            if (SECURITY.get()) {
                throwUnauthorizated();
            }
        }
        saveInContext(request, policy, schemaFactory);
    }

    protected void throwUnauthorizated() {
        throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
    }

    protected void saveInContext(ApiRequest request, Policy policy, SchemaFactory schemaFactory) {
        if (schemaFactory != null) {
            request.setSchemaFactory(schemaFactory);
        }
        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), policy.getAccountId());
        request.getServletContext().getResponse().addHeader(ACCOUNT_ID_HEADER, accountId);
        ApiContext.getContext().setPolicy(policy);
    }

    protected Policy getPolicy(AccountAccess account, ApiRequest request) {
        Policy policy = null;

        for (AuthorizationProvider auth : authorizationProviders) {
            policy = auth.getPolicy(account, request);
            if (policy != null) {
                break;
            }
        }

        return policy;
    }

    protected SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request) {
        SchemaFactory factory = null;

        for (AuthorizationProvider auth : authorizationProviders) {
            factory = auth.getSchemaFactory(account, policy, request);
            if (factory != null) {
                break;
            }
        }

        return factory;
    }

    protected AccountAccess getAccountAccess(ApiRequest request) {
        AccountAccess accountAccess = null;

        for (AccountLookup lookup : accountLookups) {
            accountAccess = lookup.getAccountAccess(request);
            if (accountAccess != null) {
                break;
            }
        }

        if (accountAccess != null) {
            return accountAccess;
        }

        if (SECURITY.get()) {
            for (AccountLookup lookup : accountLookups) {
                if (lookup.challenge(request)) {
                    break;
                }
            }
        }

        return accountAccess;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    public List<AuthorizationProvider> getAuthorizationProviders() {
        return authorizationProviders;
    }

    @Inject
    public void setAuthorizationProviders(List<AuthorizationProvider> authorizationProviders) {
        this.authorizationProviders = authorizationProviders;
    }

    public List<AccountLookup> getAccountLookups() {
        return accountLookups;
    }

    @Inject
    public void setAccountLookups(List<AccountLookup> accountLookups) {
        this.accountLookups = accountLookups;
    }

}
