package io.github.ibuildthecloud.dstack.api.auth.dao;

import io.github.ibuildthecloud.dstack.db.jooq.generated.model.Account;

public interface AuthDao {

    Account getAdminAccount();

}
