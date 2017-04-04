package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceAuthProvider;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenUtil;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.TokenAuthLookup;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.ObjectUtils;


public class TokenResourceManager extends AbstractNoOpResourceManager {

    @Inject
    AuthTokenDao authTokenDao;

    @Inject
    IdentityManager identityManager;

    @Inject
    ExternalServiceAuthProvider externalAuthProvider;

    @Inject
    TokenService tokenService;

    @Inject
    TokenAuthLookup tokenAuthLookup;

    @Inject
    AuthDao authDao;

    @Inject
    AccountDao accountDao;

    private List<TokenCreator> tokenCreators;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Token.class};
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType())) {
            return null;
        }
        return createToken(request);
    }

    private Token createToken(ApiRequest request) {
        Token token = null;
        if (SecurityConstants.AUTH_PROVIDER.get() == null || SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                    "NoAuthProvider", "No Auth provider is configured.", null);
        }

        if (SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())) {
            for (TokenCreator tokenCreator : tokenCreators) {
                if (tokenCreator.isConfigured() && tokenCreator.providerType().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
                    if (!SecurityConstants.SECURITY.get()) {
                        tokenCreator.reset();
                    }
                    token = tokenCreator.getToken(request);
                    break;
                }
            }
        } else {
            //call external service
            token = externalAuthProvider.getToken(request);
        }

        if (token == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST,
                    "codeInvalid", "Code provided is invalid.", null);
        }
        Identity[] identities = token.getIdentities();
        List<Identity> transFormedIdentities = new ArrayList<>();
        for (Identity identity : identities) {
            transFormedIdentities.add(identityManager.untransform(identity, true));
        }
        token.setIdentities(transFormedIdentities);
        token.setUserIdentity(identityManager.untransform(token.getUserIdentity(), true));

        String dbJwt = token.getJwt();
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(dbJwt, true);
        } catch (TokenException e) {
            return null;
        }
        Long authenticatedAsAccountId = null;
        if (jsonData != null) {
            String accountId = ObjectUtils.toString(jsonData.get(AbstractTokenUtil.ACCOUNT_ID), null);
            if (null == accountId) {
                return null;
            }
            TokenUtil tokenUtil = tokenAuthLookup.tokenUtils();
            Account account = authDao.getAccountByExternalId(accountId, tokenUtil.userType());
            if (account != null) {
                authenticatedAsAccountId = account.getId();
            }
        }
        long tokenAccountId = (accountDao.findByUuid(AbstractTokenUtil.TOKEN)).getId();
        deletePreviousToken(authenticatedAsAccountId, tokenAccountId);

        token.setJwt(authTokenDao.createToken(token.getJwt(), token.getAuthProvider(), ((Policy) ApiContext.getContext().getPolicy()).getAccountId(), authenticatedAsAccountId).getKey());
        return token;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Token token = new Token();

        //get RedirectUrl if applicable
        if (SecurityConstants.AUTH_PROVIDER.get() == null || SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())) {
            return token;
        }

        if (SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())) {
            return token;
        } else {
            //get redirect Url from external service
            if (externalAuthProvider.isConfigured()) {
                return externalAuthProvider.readCurrentToken();
            }
        }
        return token;
    }

    public List<TokenCreator> getTokenCreators() {
        return tokenCreators;
    }

    @Inject
    public void setTokenCreators(List<TokenCreator> tokenCreators) {
        this.tokenCreators = tokenCreators;
    }

    public void deletePreviousToken(long authenticatedAsAccountId, long tokenAccountId) {
        AuthToken authToken = authTokenDao.getTokenByAuthenticatedAsAccountId(authenticatedAsAccountId, tokenAccountId);
        if (authToken == null) {
            return;
        }
        authTokenDao.deleteToken(authToken.getKey());
    }
}
