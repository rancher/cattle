package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.core.model.Account;

import java.util.List;

public interface AuthDao {

    Account getAdminAccount();

    Account getAccountById(Long id);

    Account getAccountByKeys(String access, String secretKey);

    Account getAccountByExternalId(String externalId, String exteralType);

    Account getAccountByUuid(String uuid);

    Account createAccount(String name, String kind, String externalId, String externalType);

    void updateAccount(Account account, String name, String kind, String externalId, String externalType);

    List<? extends Account> getAccessibleProjects(String userAccountId, List<String> orgIds, List<String> teamIds);
}
