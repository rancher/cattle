package io.cattle.platform.iaas.api.auth.dao.impl;

import static io.cattle.platform.core.model.tables.AuthTokenTable.AUTH_TOKEN;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.core.model.tables.records.AuthTokenRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Date;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;


public class AuthTokenDaoImpl extends AbstractJooqDao implements AuthTokenDao{

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public AuthTokenRecord getTokenByKey(String key) {
        return create()
                .selectFrom(AUTH_TOKEN)
                .where(AUTH_TOKEN.KEY.eq(key))
                        .and(AUTH_TOKEN.VERSION.eq(SecurityConstants.TOKEN_VERSION))
                        .and(AUTH_TOKEN.EXPIRES.greaterThan(new Date()))
                                .orderBy(AUTH_TOKEN.ID.asc()).fetchOne();
    }

    @Override
    public AuthToken createToken(String jwt, String provider, long accountId) {
        if (StringUtils.isBlank(jwt)){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "NoJwtToSave", "Cannot save a null jwt.",
                    null);
        }
        AuthTokenRecord authTokenRecord = new AuthTokenRecord();
        authTokenRecord.setAccountId(accountId);
        authTokenRecord.setValue(jwt);
        authTokenRecord.setKey(ApiKeyFilter.generateKeys()[1]);
        authTokenRecord.setVersion(SecurityConstants.TOKEN_VERSION);
        authTokenRecord.setProvider(provider);
        Date expiry = new Date(System.currentTimeMillis() + SecurityConstants.TOKEN_EXPIRY_MILLIS.get());
        authTokenRecord.setCreated(new Date());
        authTokenRecord.setExpires(expiry);
        int result = create().executeInsert(authTokenRecord);
        if (result == 0){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                    "AuthTokenCreation", "Failed to create auth token.", null);
        }
        return authTokenRecord;
    }

    @Override
    public AuthToken getTokenByAccountId(long accountId) {
        return create()
                .selectFrom(AUTH_TOKEN)
                .where(AUTH_TOKEN.ACCOUNT_ID.eq(accountId))
                        .and(AUTH_TOKEN.VERSION.eq(SecurityConstants.TOKEN_VERSION))
                        .and(AUTH_TOKEN.PROVIDER.eq(SecurityConstants.AUTH_PROVIDER.get()))
                        .and(AUTH_TOKEN.EXPIRES.greaterThan(new Date()))
                .orderBy(AUTH_TOKEN.EXPIRES.desc()).fetchAny();
    }

    @Override
    public boolean deleteToken(String key) {
        return create().executeDelete(getTokenByKey(key)) == 1;
    }
}
