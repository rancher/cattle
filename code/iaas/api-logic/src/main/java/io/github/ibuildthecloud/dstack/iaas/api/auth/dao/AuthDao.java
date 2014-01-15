package io.github.ibuildthecloud.dstack.iaas.api.auth.dao;

import io.github.ibuildthecloud.dstack.core.model.Account;

public interface AuthDao {

    Account getAdminAccount();

}
