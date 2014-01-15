package io.github.ibuildthecloud.dstack.api.auth.impl;

import io.github.ibuildthecloud.dstack.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class ApiAuthenticator extends AbstractApiRequestHandler {

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");

    AuthDao authDao;
    Map<String,PolicyOptions> options = new ConcurrentHashMap<String, PolicyOptions>();

    @Override
    public void handle(ApiRequest request) throws IOException {
        if ( ApiContext.getContext().getPolicy() != null ) {
            return;
        }

        if ( SECURITY.get() ) {
//            authenticate();
        } else {
            noSecurity();
        }
    }

    protected void noSecurity() {
        Account adminAccount = authDao.getAdminAccount();
        if ( adminAccount == null )
            return;

        ApiContext.getContext().setPolicy(new AccountPolicy(adminAccount, getOptions(adminAccount)));
    }

    protected PolicyOptions getOptions(Account account) {
        String kind = account.getKind();

        PolicyOptions opts = options.get(kind);
        if ( opts != null ) {
            return opts;
        }

        opts = new ArchaiusPolicyOptions(kind);
        options.put(kind, opts);

        return opts;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

}
