package io.github.ibuildthecloud.dstack.iaas.api.auth.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.iaas.api.auth.AccountLookup;
import io.github.ibuildthecloud.dstack.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class BasicAuthImpl implements AccountLookup {

    public static final String AUTH_HEADER = "Authorization";
    public static final String CHALLENGE_HEADER = "WWW-Authenticate";
    public static final String BASIC = "Basic";
    public static final String BASIC_REALM = "Basic realm=\"%s\"";

    private static final DynamicStringProperty REALM = ArchaiusUtil.getString("api.auth.realm");

    AuthDao authDao;

    @Override
    public Account getAccount(ApiRequest request) {
        String[] auth = getUsernamePassword(request.getServletContext().getRequest().getHeader(AUTH_HEADER));

        return auth == null ? null : authDao.getAccountByKeys(auth[0], auth[1]);
    }

    @Override
    public void challenge(ApiRequest request) {
        HttpServletResponse response = request.getServletContext().getResponse();
        String realm = REALM.get();

        if ( realm == null ) {
            response.setHeader(CHALLENGE_HEADER, BASIC);
        } else {
            response.setHeader(CHALLENGE_HEADER, String.format(BASIC_REALM,realm));
        }
    }

    protected String getRealm(ApiRequest request) {
        return REALM.get();
    }

    public static String[] getUsernamePassword(String auth) {
        if ( auth == null )
            return null;

        String[] parts = StringUtils.split(auth);

        if ( parts.length != 2 ) {
            return null;
        }

        if ( ! parts[0].equalsIgnoreCase(BASIC) )
            return null;

        try {
            String text = new String(Base64.decodeBase64(parts[1]), "UTF-8");
            int i = text.indexOf(":");
            if ( i == -1 ) {
                return null;
            }

            return new String[] { text.substring(0, i), text.substring(i+1) };
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

}
