package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;

public interface ClusterDao {

    Account getOwnerAcccountForCluster(Cluster cluster);

    Account createOwnerAccount(Cluster cluster);

}
