package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.core.model.AuthToken;

public interface AuthTokenDao {

    AuthToken getTokenByKey(String key);

    AuthToken createToken(String jwt, String provider, long accountId, long authenticatedAsAccountId);

    AuthToken getTokenByAccountId(long accountId);

    void deletePreviousTokens(long authenticatedAsAccountId, long tokenAccountId);

    boolean deleteToken(String key);
}
