package io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class OpenLDAPUtils extends AbstractTokenUtil {

    @Override
    public String tokenType() {
        return OpenLDAPConstants.LDAP_JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromHashSeparatedString(OpenLDAPConstants.LDAP_ALLOWED_IDENTITIES.get());

        for (String id : idList) {
            for (String whiteId: whitelistedValues){
                if (StringUtils.equals(id, whiteId)){
                    return true;
                }
            }
        }
        return false;
    }

    public String toHashSeparatedString(List<Identity> identities) {
        StringBuilder sb = new StringBuilder();
        Iterator<Identity> identityIterator = identities.iterator();
        while (identityIterator.hasNext()){
            sb.append(identityIterator.next().getId().trim());
            if (identityIterator.hasNext()) sb.append('#');
        }
        return sb.toString();
    }

    public List<String> fromHashSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<>();
        String[] splitted = string.split("#");
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
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
