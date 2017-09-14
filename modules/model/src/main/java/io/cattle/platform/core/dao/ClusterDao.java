package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Instance;

public interface ClusterDao {

    // TODO: Cache impl
    Account getOwnerAcccountForCluster(Cluster cluster);

    // TODO: Cache impl
    Long getOwnerAcccountIdForCluster(Long clusterId);

    Account createOwnerAccount(Cluster cluster);

    Account getDefaultProject(Cluster cluster);

    Account createDefaultProject(Cluster cluster);

    Cluster assignTokens(Cluster cluster);

    Instance getAnyRancherAgent(Cluster cluster);
}
