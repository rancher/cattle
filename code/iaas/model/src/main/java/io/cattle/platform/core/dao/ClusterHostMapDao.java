package io.cattle.platform.core.dao;

import java.util.List;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;

public interface ClusterHostMapDao {

    List<ClusterHostMapRecord> findClusterHostMapsHavingHost(Host host);

    List<ClusterHostMapRecord> findClusterHostMapsForCluster(Host cluster);

    ClusterHostMapRecord getClusterHostMap(Host cluster, Host host);
}
