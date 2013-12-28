package io.github.ibuildthecloud.dstack.process.dao;

import io.github.ibuildthecloud.dstack.core.model.Account;

public interface AccountDao {

    Account findByUuid(String uuid);

}
