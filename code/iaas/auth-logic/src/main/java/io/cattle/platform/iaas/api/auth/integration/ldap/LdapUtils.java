package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.iaas.api.auth.TokenUtils;

import java.util.List;

public class LdapUtils extends TokenUtils {

    @Override
    protected String getAccountType() {
        return LdapConstants.USER_SCOPE;
    }

    @Override
    protected String tokenType() {
        return LdapConstants.LDAP_JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        //TODO Real white listing needed.
        return true;
    }

    @Override
    protected String accessMode() {
        return LdapConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return LdapConstants.LDAP_ACCESS_TOKEN;
    }
}
