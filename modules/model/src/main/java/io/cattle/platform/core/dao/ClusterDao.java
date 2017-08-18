package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;

public interface ClusterDao {

    // TODO: Cache impl
    Account getOwnerAcccountForCluster(Cluster cluster);

    // TODO: Cache impl
    Long getOwnerAcccountIdForCluster(Long clusterId);

    Account createOwnerAccount(Cluster cluster);

    Cluster createClusterForAccount(Account account, K8sClientConfig clientConfig);

    Cluster assignTokens(Cluster cluster);

}
