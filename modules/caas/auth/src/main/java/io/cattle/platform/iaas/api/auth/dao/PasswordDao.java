package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.iaas.api.auth.integration.local.ChangePassword;

public interface PasswordDao {
    Credential changePassword(Credential password, ChangePassword changePassword, Policy policy);

    Credential createAdminAccountAndPassword(String username, String password, String name);

    boolean isUnique(Credential credential);

    void verifyUsernamePassword(String username, String password, String name);
}
