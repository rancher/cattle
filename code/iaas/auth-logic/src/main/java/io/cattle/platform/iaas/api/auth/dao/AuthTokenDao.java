package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.core.model.AuthToken;

public interface AuthTokenDao {

    AuthToken getTokenByKey(String key);

    AuthToken createToken(String jwt, String provider, long accountId);

    AuthToken getTokenByAccountId(long accountId);

    boolean deleteToken(String key);
}
