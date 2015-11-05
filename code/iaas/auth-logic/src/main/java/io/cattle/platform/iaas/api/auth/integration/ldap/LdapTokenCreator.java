package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
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

public class LdapTokenCreator extends LdapConfigurable implements TokenCreator {

    @Inject
    LdapIdentitySearchProvider ldapIdentitySearchProvider;
    @Inject
    AuthDao authDao;
    @Inject
    TokenService tokenService;
    @Inject
    ProjectResourceManager projectResourceManager;
    @Inject
    ObjectManager objectManager;

    @Inject
    LdapUtils ldapUtils;

    private Token getLdapToken(String username, String password) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, LdapConstants.CONFIG, "Ldap Not Configured.", null);
        }
        return ldapUtils.createToken(ldapIdentitySearchProvider.getIdentities(username, password), null);
    }

    @Override
    public Token getToken(ApiRequest request) {
        Map<String, Object> requestBody = CollectionUtils.toMap(request.getRequestObject());
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "LdapConfig", "LdapConfig is not Configured.", null);
        }
        String code = ObjectUtils.toString(requestBody.get(SecurityConstants.CODE));
        String[] split = code.split(":");
        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        return getLdapToken(split[0], split[1]);
    }

    @Override
    public String providerType() {
        return LdapConstants.CONFIG;
    }

    @Override
    public String getName() {
        return LdapConstants.TOKEN_CREATOR;
    }
}
