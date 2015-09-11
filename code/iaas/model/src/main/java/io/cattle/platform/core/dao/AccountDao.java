package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;

public interface AccountDao {

    Account getSystemAccount();

    Credential getApiKey(Account account, boolean requireActive);

    Account findByUuid(String uuid);

    void deleteProjectMemberEntries(Account account);
}
