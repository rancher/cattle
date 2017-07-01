package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;

import java.util.List;

public interface AccountDao {

    Account getSystemAccount();

    List<? extends Credential> getApiKeys(Account account, String kind, boolean requireActive);

    Account findByUuid(String uuid);

    void deleteProjectMemberEntries(Account account);

    Account getAdminAccountExclude(long accountId);

    Account getAccountById(Long id);

    boolean isActiveAccount(Account account);

    List<String> getAccountActiveStates();

    void generateAccountLinks(Account account, List<? extends Long> links);

    // links for the accounts linked to target account both ways
    List<Long> getLinkedAccounts(long accountId);

}
