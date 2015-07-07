package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.iaas.api.auth.ExternalIdHandler;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class ApiAuthenticator extends AbstractApiRequestHandler {

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final Logger log = LoggerFactory.getLogger(ApiAuthenticator.class);

    private static final String ACCOUNT_ID_HEADER = "X-API-ACCOUNT-ID";

    AuthDao authDao;
    List<AccountLookup> accountLookups;
    List<ExternalIdHandler> externalIdHandlers;
    List<AuthorizationProvider> authorizationProviders;
    @Inject
    ObjectManager objectManager;
    @Override
    public void handle(ApiRequest request) throws IOException {
        if (ApiContext.getContext().getPolicy() != null) {
            return;
        }

        Account authenticatedAsAccount = getAccount(request);
        if (authenticatedAsAccount == null) {
            throwUnauthorizated();
        }

        Set<ExternalId> externalIds = getExternalIds(authenticatedAsAccount);

        Account account = getAccountRequested(authenticatedAsAccount, externalIds, request);

        Policy policy = getPolicy(account, authenticatedAsAccount, externalIds, request);
        if (policy == null) {
            log.error("Failed to find policy for [{}]", account.getId());
            throwUnauthorizated();
        }

        SchemaFactory schemaFactory = getSchemaFactory(account, policy, request);
        if (schemaFactory == null) {
            log.error("Failed to find a schema for account type [{}]", account.getKind());
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

    protected Policy getPolicy(Account account, Account authenticatedAsAccount, Set<ExternalId> externalIds, ApiRequest request) {
        Policy policy = null;

        for (AuthorizationProvider auth : authorizationProviders) {
            policy = auth.getPolicy(account, authenticatedAsAccount, externalIds, request);
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

    protected Account getAccount(ApiRequest request) {
        Account account = null;

        for (AccountLookup lookup : accountLookups) {
            account = lookup.getAccount(request);
            if (account != null) {
                break;
            }
        }

        if (account != null) {
            return account;
        }

        if (SECURITY.get()) {
            for (AccountLookup lookup : accountLookups) {
                if (lookup.challenge(request)) {
                    break;
                }
            }
        }

        return account;
    }

    private Set<ExternalId> getExternalIds(Account account) {
        Set<ExternalId> externalIds = new HashSet<>();

        for (ExternalIdHandler externalIdHandler : externalIdHandlers) {
            externalIds.addAll(externalIdHandler.getExternalIds(account));
        }

        return externalIds;
    }

    private Account getAccountRequested(Account authenticatedAsAccount, Set<ExternalId> externalIds, ApiRequest request) {
        Account project;

        String projectId = request.getServletContext().getRequest().getHeader(ProjectConstants.PROJECT_HEADER);
        if (projectId == null || projectId.isEmpty()) {
            projectId = request.getServletContext().getRequest().getParameter("projectId");
        }
        if (projectId == null || projectId.isEmpty()) {
            projectId = (String) request.getAttribute(ProjectConstants.PROJECT_HEADER);
        }

        if (projectId == null || projectId.isEmpty()) {
            return authenticatedAsAccount;
        }

        String unobfuscatedId;

        try {
            unobfuscatedId = ApiContext.getContext().getIdFormatter().parseId(projectId);
        } catch (NumberFormatException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "projectId header format is incorrect " + projectId, null);
        }

        if (StringUtils.isEmpty(unobfuscatedId)) {
            return null;
        }
        try{
            project = authDao.getAccountById(new Long(unobfuscatedId));
            if (project == null || !project.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)){
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
            if (authenticatedAsAccount.getId() == project.getId()){
                return authenticatedAsAccount;
            }
        } catch (NumberFormatException e){
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        Policy tempPolicy = getPolicy(authenticatedAsAccount, authenticatedAsAccount, externalIds, request);
        if (project != null && authDao.hasAccessToProject(project.getId(), authenticatedAsAccount.getId(),
                tempPolicy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), externalIds)) {
            return project;
        }
        throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
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

    public List<ExternalIdHandler> getExternalIdHandlers() {
        return externalIdHandlers;
    }

    @Inject
    public void setExternalIdHandlers(List<ExternalIdHandler> externalIdHandlers) {
        this.externalIdHandlers = externalIdHandlers;
    }

}
