package io.cattle.platform.register.auth;

import io.cattle.platform.core.model.Account;

public interface RegistrationAuthTokenManager {

    Account validateToken(String token);

}
