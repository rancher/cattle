package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetaHelperInfo {
    Map<Long, List<HealthcheckInstanceHostMap>> instanceIdToHealthCheckers;
    Map<Long, HostMetaData> hostIdToHostMetadata;
    Map<Long, Account> accounts;
    Set<Long> otherAccountsServicesIds;
    Set<Long> otherAccountsStackIds;
    Account account;

    public MetaHelperInfo(Account account, Map<Long, Account> accounts, Set<Long> otherAccountsServicesIds,
            Set<Long> otherAccountsStackIds,
            MetaDataInfoDao dao) {
        super();
        this.accounts = accounts;
        this.otherAccountsServicesIds = otherAccountsServicesIds;
        this.otherAccountsStackIds = otherAccountsStackIds;
        this.account = account;
        // may be fix in the future - get health checker hosts ids for instances of other accounts
        // otherwise this info is irrelevant
        this.instanceIdToHealthCheckers = dao.getInstanceIdToHealthCheckers(account);
        this.hostIdToHostMetadata = dao.getHostIdToHostMetadata(account, accounts, otherAccountsServicesIds);
    }

    public Map<Long, List<HealthcheckInstanceHostMap>> getInstanceIdToHealthCheckers() {
        return instanceIdToHealthCheckers;
    }

    public Map<Long, HostMetaData> getHostIdToHostMetadata() {
        return hostIdToHostMetadata;
    }

    public Map<Long, Account> getAccounts() {
        return accounts;
    }

    public Account getAccount() {
        return account;
    }

    public Set<Long> getOtherAccountsServicesIds() {
        return otherAccountsServicesIds;
    }

    public Set<Long> getOtherAccountsStackIds() {
        return otherAccountsStackIds;
    }
}
