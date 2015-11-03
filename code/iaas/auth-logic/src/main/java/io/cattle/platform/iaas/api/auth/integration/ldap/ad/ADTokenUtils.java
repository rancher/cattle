package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;

import java.util.List;

public class ADTokenUtils extends AbstractTokenUtil {

    @Override
    public String tokenType() {
        return ADConstants.LDAP_JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        //TODO Real white listing needed.
        return true;
    }

    @Override
    protected String accessMode() {
        return ADConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return ADConstants.LDAP_ACCESS_TOKEN;
    }

    @Override
    protected void postAuthModification(Account account) {

    }

    @Override
    public String userType() {
        return ADConstants.USER_SCOPE;
    }

    @Override
    public boolean createAccount() {
        return true;
    }

    @Override
    public String getName() {
        return ADConstants.CONFIG;
    }
}
