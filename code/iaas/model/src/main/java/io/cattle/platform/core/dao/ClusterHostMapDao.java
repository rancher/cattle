package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;

import java.util.List;

public interface ClusterHostMapDao {

    List<ClusterHostMapRecord> findClusterHostMapsHavingHost(Host host);

    List<ClusterHostMapRecord> findClusterHostMapsForCluster(Host cluster);

    ClusterHostMapRecord getClusterHostMap(Host cluster, Host host);

    IpAddress getIpAddressForHost(Long hostId);

    Host findHostByName(Long accountId, String name);
}
