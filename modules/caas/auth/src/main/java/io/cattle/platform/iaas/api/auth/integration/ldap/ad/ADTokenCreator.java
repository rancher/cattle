package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextCreationException;
import io.cattle.platform.iaas.api.auth.integration.ldap.ServiceContextRetrievalException;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;
import java.util.Objects;

public class ADTokenCreator extends ADConfigurable implements TokenCreator {

    ADIdentityProvider adIdentityProvider;
    ADTokenUtils adUtils;

    public ADTokenCreator(ADIdentityProvider adIdentityProvider, ADTokenUtils adUtils) {
        this.adIdentityProvider = adIdentityProvider;
        this.adUtils = adUtils;
    }

    private Token getLdapToken(String username, String password) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, ADConstants.CONFIG, "Ldap Not Configured.", null);
        }
        try {
            return adUtils.createToken(adIdentityProvider.getIdentities(username, password), null);
        } catch (ServiceContextCreationException | ServiceContextRetrievalException e){
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "LdapDown", "Could not talk to ldap", null);
        }
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "ADConfig", "ADConfig is not Configured.", null);
        }
        String code = Objects.toString(requestBody.get(SecurityConstants.CODE), null);
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

    @Override
    public String getName() {
        return ADConstants.TOKEN_CREATOR;
    }

    @Override
    public Token getCurrentToken() {
        return adUtils.retrieveCurrentToken();
    }
}
