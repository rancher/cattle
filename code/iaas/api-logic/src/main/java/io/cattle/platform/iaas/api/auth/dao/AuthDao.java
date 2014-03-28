package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.core.model.Account;

public interface AuthDao {

    Account getAdminAccount();

    Account getAccountByKeys(String access, String secretKey);

}
