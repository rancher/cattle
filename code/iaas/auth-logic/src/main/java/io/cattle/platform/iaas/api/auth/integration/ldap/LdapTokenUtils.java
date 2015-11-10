package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;

import java.util.List;

public class LdapTokenUtils extends AbstractTokenUtil {

    @Override
    public String tokenType() {
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

    @Override
    protected void postAuthModification(Account account) {

    }

    @Override
    public String userType() {
        return LdapConstants.USER_SCOPE;
    }

    @Override
    public boolean createAccount() {
        return true;
    }

    @Override
    public String getName() {
        return LdapConstants.CONFIG;
    }
}
