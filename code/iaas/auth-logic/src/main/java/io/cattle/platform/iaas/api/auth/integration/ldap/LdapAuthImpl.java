package io.cattle.platform.iaas.api.auth.integration.ldap;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LdapAuthImpl extends LdapConfigurable implements AccountLookup, Priority {

    @Inject
    LdapUtils ldapUtils;

    @Override
    public Account getAccount(ApiRequest request) {
        if (StringUtils.equals(LdapConstants.TOKEN, request.getType())) {
            return null;
        }
        ldapUtils.findAndSetJWT();
        return getAccountAccessInternal();
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    public Account getAccountAccess(String token, ApiRequest request) {
        request.setAttribute(LdapConstants.LDAP_JWT, token);
        return getAccountAccessInternal();
    }

    private Account getAccountAccessInternal() {
        return ldapUtils.getAccountFromJWT();
    }

    @Override
    public String getName() {
        return "LdapAuthImpl";
    }
}
