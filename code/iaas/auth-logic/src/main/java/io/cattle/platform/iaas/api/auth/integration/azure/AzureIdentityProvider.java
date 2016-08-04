package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class AzureIdentityProvider extends AzureConfigurable implements IdentityProvider {

    @Inject
    AzureRESTClient azureClient;

    @Inject
    private AzureTokenUtil azureTokenUtils;
    @Inject
    private AuthTokenDao authTokenDao;
    @Inject
    AzureTokenCreator azureTokenCreator;

    @Override
    public List<Identity> searchIdentities(String name, boolean exactMatch) {
        if (!isConfigured()) {
            notConfigured();
        }
        List<Identity> identities = new ArrayList<>();
        for (String scope : scopes()) {
            identities.addAll(searchIdentities(name, scope, exactMatch));
        }
        return identities;
    }

    @Override
    public List<Identity> searchIdentities(String name, String scope, boolean exactMatch) {
        //TODO:Implement exact match.
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope){
            case AzureConstants.USER_SCOPE:
                return searchUsers(name, exactMatch);
            case AzureConstants.GROUP_SCOPE:
                return searchGroups(name, exactMatch);
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public Set<Identity> getIdentities(Account account) {
        if (!isConfigured() ||
                !AzureConstants.USER_SCOPE.equalsIgnoreCase(account.getExternalIdType())) {
            return new HashSet<>();
        }
        String accessToken = (String) DataAccessor.fields(account).withKey(AzureConstants.AZURE_ACCESS_TOKEN).get();
        String refreshToken = (String) DataAccessor.fields(account).withKey(AzureConstants.AZURE_REFRESH_TOKEN).get();
        if (azureTokenUtils.findAndSetJWT()){
            ApiRequest request = ApiContext.getContext().getApiRequest();
            request.setAttribute(AzureConstants.AZURE_ACCESS_TOKEN, accessToken);
            request.setAttribute(AzureConstants.AZURE_REFRESH_TOKEN, refreshToken);
            return azureTokenUtils.getIdentities();
        }
        String jwt = null;
        if (!StringUtils.isBlank(accessToken) && SecurityConstants.SECURITY.get()) {
            AuthToken authToken = authTokenDao.getTokenByAccountId(account.getId());
            if (authToken == null) {
                try {
                    jwt = ProjectConstants.AUTH_TYPE + azureTokenCreator.getAzureToken(accessToken).getJwt();
                    authToken = authTokenDao.createToken(jwt, AzureConstants.CONFIG, account.getId());
                    jwt = authToken.getKey();
                } catch (ClientVisibleException e) {
                    if (e.getCode().equalsIgnoreCase(AzureConstants.AZURE_ERROR) &&
                            !e.getDetail().contains("401")) {
                        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                                AzureConstants.JWT_CREATION_FAILED, "", null);
                    }
                }
            } else {
                jwt = authToken.getKey();
            }

        }
        if (StringUtils.isBlank(jwt)){
            return Collections.emptySet();
        }
        ApiRequest request = ApiContext.getContext().getApiRequest();
        request.setAttribute(AzureConstants.AZURE_JWT, jwt);
        request.setAttribute(AzureConstants.AZURE_ACCESS_TOKEN, accessToken);
        request.setAttribute(AzureConstants.AZURE_REFRESH_TOKEN, refreshToken);
        return azureTokenUtils.getIdentities();
    }

    private List<Identity> searchGroups(String groupName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();

        AzureAccountInfo group;
        try {
            group =  azureClient.getAzureGroupByName(groupName);
            if (group == null){
                return identities;
            }
        } catch (ClientVisibleException e) {
            return identities;
        }
        Identity identity = group.toIdentity(AzureConstants.GROUP_SCOPE);
        identities.add(identity);
        return identities;

    }

    private List<Identity> searchUsers(String userName, boolean exactMatch) {
        List<Identity> identities = new ArrayList<>();

        AzureAccountInfo user;
        try {
            user =  azureClient.getAzureUserByName(userName);
        } catch (ClientVisibleException e) {
            return identities;
        }
        if (user == null) {
            return identities;
        }
        Identity identity = user.toIdentity(AzureConstants.USER_SCOPE);
        identities.add(identity);
        return identities;

    }

    @Override
    public Identity getIdentity(String id, String scope) {
        if (!isConfigured()){
            notConfigured();
        }
        switch (scope) {
            case AzureConstants.USER_SCOPE:
                AzureAccountInfo user = azureClient.getUserById(id);
                return user.toIdentity(AzureConstants.USER_SCOPE);
            case AzureConstants.GROUP_SCOPE:
                AzureAccountInfo org = azureClient.getGroupById(id);
                return org.toIdentity(AzureConstants.GROUP_SCOPE);
            default:
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                        IdentityConstants.INVALID_TYPE, "Invalid scope for AzureSearchProvider", null);
        }
    }

    private void notConfigured() {
        throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE,
                "NotConfigured", "Azure is not configured", null);
    }


    @Override
    public Set<String> scopes() {
        return AzureConstants.SCOPES;
    }

    @Override
    public String getName() {
        return AzureConstants.SEARCH_PROVIDER;
    }


    @Override
    public Identity transform(Identity identity) {
        if (scopes().contains(identity.getExternalIdType())) {
            return getIdentity(identity.getExternalId(), identity.getExternalIdType());
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
            "Azure does not provide: " + identity.getExternalIdType(), null );
    }

    @Override
    public Identity untransform(Identity identity) {
        if (scopes().contains(identity.getExternalIdType())) {
            return identity;
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, IdentityConstants.INVALID_TYPE,
            "Azure does not provide: " + identity.getExternalIdType(), null );
    }
}
