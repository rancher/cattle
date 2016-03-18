package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiAuthenticator extends AbstractApiRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiAuthenticator.class);

    private static final String ACCOUNT_ID_HEADER = "X-API-ACCOUNT-ID";

    AuthDao authDao;
    List<AccountLookup> accountLookups;
    List<IdentityProvider> identityProviders;
    List<AuthorizationProvider> authorizationProviders;
    @Inject
    ObjectManager objectManager;

    @Inject
    TransformationService transformationService;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (ApiContext.getContext().getPolicy() != null) {
            return;
        }
        if (ApiContext.getContext().getTransformationService() == null){
            ApiContext.getContext().setTransformationService(transformationService);
        }

        Account authenticatedAsAccount = getAccount(request);
        if (authenticatedAsAccount == null ||
                !StringUtils.equals(CommonStatesConstants.ACTIVE, authenticatedAsAccount.getState())) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }

        Set<Identity> identities = getIdentities(authenticatedAsAccount);
        if (identities == null || identities.size() == 0) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }

        Account account = getAccountRequested(authenticatedAsAccount, identities, request);
        Policy policy = getPolicy(account, authenticatedAsAccount, identities, request);
        if (policy == null) {
            log.error("Failed to find policy for [{}]", account.getId());
            throwUnauthorized();
        }

        SchemaFactory schemaFactory = getSchemaFactory(account, policy, request);
        if (schemaFactory == null) {
            log.error("Failed to find a schema for account type [{}]", account.getKind());
            if (SecurityConstants.SECURITY.get()) {
                throwUnauthorized();
            }
        } else if (request.getType() != null && request.getType().endsWith("s")) {
            //In case the SchemaFactory didn't have the type before.
            //we need to make it singular.
            String singleType = schemaFactory.getSingularName(request.getType());
            if (singleType != null) {
                request.setType(singleType);
            }
        }
        saveInContext(request, policy, schemaFactory);
    }

    protected void throwUnauthorized() {
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

    protected Policy getPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        Policy policy = null;

        for (AuthorizationProvider auth : authorizationProviders) {
            policy = auth.getPolicy(account, authenticatedAsAccount, identities, request);
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
            if (lookup.isConfigured()){
                account = lookup.getAccount(request);
                if (account != null) {
                    request.setAttribute(AccountConstants.AUTH_TYPE, lookup.getName());
                    break;
                }
            }
        }

        if (account != null) {
            return account;
        }

        if (SecurityConstants.SECURITY.get()) {
            for (AccountLookup lookup : accountLookups) {
                if (lookup.challenge(request)) {
                    break;
                }
            }
        }

        return null;
    }

    private Set<Identity> getIdentities(Account account) {
        Set<Identity> identities = new HashSet<>();

        for (IdentityProvider identityProvider : identityProviders) {
            identities.addAll(identityProvider.getIdentities(account));
        }
        identities.remove(null);
        return identities;
    }

    private Account getAccountRequested(Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        Account project;
        String parsedProjectId = null;

        String projectId = request.getServletContext().getRequest().getHeader(ProjectConstants.PROJECT_HEADER);
        if (projectId == null || projectId.isEmpty()) {
            projectId = request.getServletContext().getRequest().getParameter("projectId");
        }
        if (projectId == null || projectId.isEmpty()) {
            projectId = (String) request.getAttribute(ProjectConstants.PROJECT_HEADER);
        }

        if (projectId == null || projectId.isEmpty()) {
            String accessKey = request.getServletContext().getRequest().getHeader(ProjectConstants.CLIENT_ACCESS_KEY);
            if (StringUtils.isNotBlank(accessKey)) {
                Account account = authDao.getAccountByAccessKey(accessKey);
                if (account != null) {
                    parsedProjectId = account.getId().toString();
                }
            }
        }

        if (StringUtils.isBlank(projectId) && StringUtils.isBlank(parsedProjectId)) {
            return authenticatedAsAccount;
        }

        try {
            if (parsedProjectId == null) {
                parsedProjectId = ApiContext.getContext().getIdFormatter().parseId(projectId);
            }
        } catch (NumberFormatException e) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidFormat", "projectId header format is incorrect " + projectId, null);
        }

        if (StringUtils.isEmpty(parsedProjectId)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        try {
            project = authDao.getAccountById(new Long(parsedProjectId));
            if (project == null || !project.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)){
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
            if (authenticatedAsAccount.getId().equals(project.getId())) {
                return authenticatedAsAccount;
            }
        } catch (NumberFormatException e) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        Policy tempPolicy = getPolicy(authenticatedAsAccount, authenticatedAsAccount, identities, request);
        if (authDao.hasAccessToProject(project.getId(), authenticatedAsAccount.getId(),
                tempPolicy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS), identities)) {
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

    public List<IdentityProvider> getIdentityProviders() {
        return identityProviders;
    }

    @Inject
    public void setIdentityProviders(List<IdentityProvider> identityProviders) {
        this.identityProviders = identityProviders;
    }

}
