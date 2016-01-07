package io.cattle.platform.iaas.api.auth.integration.local;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentityProvider;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

public class LocalAuthTokenCreator extends LocalAuthConfigurable implements TokenCreator {

    @Inject
    AuthDao authDao;
    @Inject
    LocalAuthTokenUtils localAuthTokenUtils;
    @Inject
    RancherIdentityProvider rancherIdentityProvider;

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

        Account account =authDao.getAccountByLogin(split[0], split[1], ApiContext.getContext().getTransformationService());

        if (account == null){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }

        Identity user = rancherIdentityProvider.getIdentity(String.valueOf(account.getId()), ProjectConstants.RANCHER_ID);
        user = rancherIdentityProvider.transform(user);
        Set<Identity> identities = new HashSet<>();
        identities.add(user);
        return localAuthTokenUtils.createToken(identities, account);
    }

    @Override
    public void reset() {

    }

    @Override
    public String getName() {
        return LocalAuthConstants.TOKEN_CREATOR;
    }
}
