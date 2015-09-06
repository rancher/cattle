package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.TokenUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentitySearchProvider;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentityTransformationHandler;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

public class LocalAuthTokenCreator extends LocalAuthConfigurable implements TokenCreator {

    @Inject
    AuthDao authDao;
    @Inject
    TokenService tokenService;
    @Inject
    LocalAuthUtils localAuthUtils;
    @Inject
    ObjectManager objectManager;
    @Inject
    RancherIdentitySearchProvider rancherIdentitySearchProvider;
    @Inject
    RancherIdentityTransformationHandler rancherIdentityTransformationHandler;

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());

        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "LocalAuthConfig", "LocalAuthConfig is not Configured.", null);
        }

        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String[] split = code.split(":", 2);

        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        Account account =authDao.getAccountByLogin(split[0], split[1]);

        if (account == null){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }

        Identity user = rancherIdentitySearchProvider.getIdentity(String.valueOf(account.getId()), ProjectConstants.RANCHER_ID);
        user = rancherIdentityTransformationHandler.transform(user);
        Set<Identity> identities = new HashSet<>();
        identities.add(user);
        account = localAuthUtils.getOrCreateAccount(user, identities, account, false);

        if (account == null){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "FailedToGetAccount");
        }

        authDao.updateAccount(account, account.getName(), account.getKind(), user.getExternalId(), user.getExternalIdType());

        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(TokenUtils.TOKEN, LocalAuthConstants.JWT);
        jsonData.put(TokenUtils.ACCOUNT_ID, user.getExternalId());
        jsonData.put(TokenUtils.ID_LIST, localAuthUtils.identitiesToIdList(identities));

        account = objectManager.reload(account);

        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + SecurityConstants.TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        user = rancherIdentityTransformationHandler.untransform(user);

        return new Token(jwt, SecurityConstants.AUTH_PROVIDER.get(), accountId, user,
                new ArrayList<>(identities), SecurityConstants.SECURITY.get(), account.getKind());
    }

    @Override
    public String getName() {
        return LocalAuthConstants.TOKEN_CREATOR;
    }

    @Override
    public String providerType() {
        return LocalAuthConstants.CONFIG;
    }
}
