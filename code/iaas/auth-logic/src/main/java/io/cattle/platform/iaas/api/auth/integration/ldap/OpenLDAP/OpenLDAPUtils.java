package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;

import java.util.List;

public class OpenLDAPUtils extends AbstractTokenUtil {

    @Override
    public String tokenType() {
        return OpenLDAPConstants.LDAP_JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        //TODO Real white listing needed.
        return true;
    }

    @Override
    protected String accessMode() {
        return OpenLDAPConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return OpenLDAPConstants.LDAP_ACCESS_TOKEN;
    }

    @Override
    protected void postAuthModification(Account account) {

    }

    @Override
    public String userType() {
        return OpenLDAPConstants.USER_SCOPE;
    }

    @Override
    public boolean createAccount() {
        return true;
    }

    @Override
    public String getName() {
        return OpenLDAPConstants.CONFIG;
    }
}
