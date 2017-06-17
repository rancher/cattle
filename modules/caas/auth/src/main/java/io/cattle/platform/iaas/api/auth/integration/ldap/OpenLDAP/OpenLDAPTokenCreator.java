package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

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

public class OpenLDAPTokenCreator extends OpenLDAPConfigurable implements TokenCreator {

    OpenLDAPIdentityProvider openLDAPIdentityProvider;
    OpenLDAPUtils OpenLDAPUtils;

    public OpenLDAPTokenCreator(OpenLDAPIdentityProvider openLDAPIdentityProvider,
            io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPUtils openLDAPUtils) {
        super();
        this.openLDAPIdentityProvider = openLDAPIdentityProvider;
        OpenLDAPUtils = openLDAPUtils;
    }

    private Token getLdapToken(String username, String password) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, OpenLDAPConstants.CONFIG, "Ldap Not Configured.", null);
        }
        try{
            return OpenLDAPUtils.createToken(openLDAPIdentityProvider.getIdentities(username, password), null);
        } catch (ServiceContextCreationException | ServiceContextRetrievalException e){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "LdapDown", "Could not talk to ldap", null);
        }
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "OpenLDAPConfig", "OpenLDAPConfig is not Configured.", null);
        }
        String code = Objects.toString(requestBody.get(SecurityConstants.CODE), null);
        String[] split = code.split(":", 2);
        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return getLdapToken(split[0], split[1]);
    }

    @Override
    public void reset() {
        openLDAPIdentityProvider.reset();
    }

    @Override
    public String getName() {
        return OpenLDAPConstants.TOKEN_CREATOR;
    }

    @Override
    public Token getCurrentToken() {
        return OpenLDAPUtils.retrieveCurrentToken();
    }
}
