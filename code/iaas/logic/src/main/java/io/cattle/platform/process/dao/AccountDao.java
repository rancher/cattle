package io.cattle.platform.process.dao;

import io.cattle.platform.core.model.Account;

public interface AccountDao {

    Account findByUuid(String uuid);

}
