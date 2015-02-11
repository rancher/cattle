package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AccountLookup;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class BasicAuthImpl implements AccountLookup, Priority {

    public static final String AUTH_HEADER = "Authorization";
    public static final String CHALLENGE_HEADER = "WWW-Authenticate";
    public static final String BASIC = "Basic";
    public static final String BASIC_REALM = "Basic realm=\"%s\"";
    private static final String NO_CHALLENGE_HEADER = "X-API-No-Challenge";

    private static final DynamicStringProperty REALM = ArchaiusUtil.getString("api.auth.realm");

    AuthDao authDao;

    @Override
    public Account getAccount(ApiRequest request) {
        String[] auth = getUsernamePassword(request.getServletContext().getRequest().getHeader(AUTH_HEADER));

        return auth == null ? null : authDao.getAccountByKeys(auth[0], auth[1]);
    }

    @Override
    public boolean challenge(ApiRequest request) {
        if (StringUtils.equals("true", request.getServletContext().getRequest().getHeader(NO_CHALLENGE_HEADER))) {
            return false;
        }
        HttpServletResponse response = request.getServletContext().getResponse();
        String realm = REALM.get();

        if ( realm == null ) {
            response.setHeader(CHALLENGE_HEADER, BASIC);
        } else {
            response.setHeader(CHALLENGE_HEADER,String.format(BASIC_REALM, realm));
        }

        return true;
    }

    protected String getRealm(ApiRequest request) {
        return REALM.get();
    }

    public static String[] getUsernamePassword(ApiRequest request) {
        return getUsernamePassword(request.getServletContext().getRequest().getHeader(AUTH_HEADER));
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

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

}
