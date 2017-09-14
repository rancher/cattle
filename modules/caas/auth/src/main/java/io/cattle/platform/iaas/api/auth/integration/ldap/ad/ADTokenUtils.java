package io.cattle.platform.iaas.api.auth.integration.ldap.ad;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ADTokenUtils extends AbstractTokenUtil {

    public ADTokenUtils(AuthDao authDao, TokenService tokenService, AuthTokenDao authTokenDao, ObjectManager objectManager, SettingDao settingsUtils,
            AccountDao accountDao) {
        super(authDao, tokenService, authTokenDao, objectManager, settingsUtils, accountDao);
    }

    @Override
    public String tokenType() {
        return ADConstants.LDAP_JWT;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromHashSeparatedString(ADConstants.AD_ALLOWED_IDENTITIES.get());

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
