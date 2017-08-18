package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;

import java.util.List;

public interface AccountDao {

    List<? extends Credential> getApiKeys(Account account, String kind, boolean requireActive);

    Account findByUuid(String uuid);

    void deleteProjectMemberEntries(Account account);

    Account getAdminAccountExclude(long accountId);

    Account getAccountById(Long id);

    boolean isActiveAccount(Account account);

    List<String> getAccountActiveStates();

    Long getAccountIdForCluster(Long clusterId);

}
