package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextCreationException;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextRetrievalException;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

public class ADTokenCreator extends ADConfigurable implements TokenCreator {

    @Inject
    ADIdentityProvider adIdentityProvider;
    @Inject
    AuthDao authDao;
    @Inject
    TokenService tokenService;
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;

    @Inject
    ADTokenUtils adUtils;

    private Token getLdapToken(String username, String password) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, ADConstants.CONFIG, "Ldap Not Configured.", null);
        }
        try {
            return adUtils.createToken(adIdentityProvider.getIdentities(username, password), null);
        } catch (ServiceContextCreationException | ServiceContextRetrievalException e){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not talk to ldap", null);
        }
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "ADConfig", "ADConfig is not Configured.", null);
        }
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String[] split = code.split(":", 2);
        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "MalformedCode");
        }
        return getLdapToken(split[0], split[1]);
    }

    @Override
    public void reset() {
        adIdentityProvider.reset();
    }

    public String getName() {
        return ADConstants.TOKEN_CREATOR;
    }
}
