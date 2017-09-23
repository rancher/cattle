package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Instance;

public interface ClusterDao {

    // TODO: Cache impl
    Account getOwnerAcccountForCluster(Long clusterId);

    // TODO: Cache impl
    Long getOwnerAcccountIdForCluster(Long clusterId);

    Account createOwnerAccount(Cluster cluster);

    Account createOrGetProjectByName(Cluster cluster, String name, String externalId);

    Account getDefaultProject(Cluster cluster);

    Account createDefaultProject(Cluster cluster);

    Cluster assignTokens(Cluster cluster);

    Instance getAnyRancherAgent(Cluster cluster);
}
