package io.github.ibuildthecloud.dstack.api.auth.impl;

import io.github.ibuildthecloud.dstack.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.db.jooq.generated.model.Account;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class ApiAuthenticator extends AbstractApiRequestHandler {

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBooleanProperty("api.security.enabled");

    AuthDao authDao;

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

        ApiContext.getContext().setPolicy(new AccountPolicy(adminAccount));
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

}
