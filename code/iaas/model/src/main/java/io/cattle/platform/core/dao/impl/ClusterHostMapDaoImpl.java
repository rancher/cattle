package io.cattle.platform.core.dao.impl;

import java.util.List;

import static io.cattle.platform.core.model.tables.ClusterHostMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class ClusterHostMapDaoImpl extends AbstractJooqDao implements ClusterHostMapDao {

    @Override
    public List<ClusterHostMapRecord> findClusterHostMapsHavingHost(Host host) {
        return create()
            .select(CLUSTER_HOST_MAP.fields())
                .from(CLUSTER_HOST_MAP)
                .where(CLUSTER_HOST_MAP.HOST_ID.eq(host.getId()))
                    .and(CLUSTER_HOST_MAP.REMOVED.isNull())
                .fetchInto(ClusterHostMapRecord.class);
    }

    @Override
    public List<ClusterHostMapRecord> findClusterHostMapsForCluster(Host cluster) {
        return create()
            .select(CLUSTER_HOST_MAP.fields())
                .from(CLUSTER_HOST_MAP)
                .where(CLUSTER_HOST_MAP.CLUSTER_ID.eq(cluster.getId()))
                    .and(CLUSTER_HOST_MAP.REMOVED.isNull())
                .fetchInto(ClusterHostMapRecord.class);
    }

    @Override
    public ClusterHostMapRecord getClusterHostMap(Host cluster, Host host) {
        return create()
            .select(CLUSTER_HOST_MAP.fields())
                .from(CLUSTER_HOST_MAP)
                .where(CLUSTER_HOST_MAP.CLUSTER_ID.eq(cluster.getId()))
                    .and(CLUSTER_HOST_MAP.HOST_ID.eq(host.getId()))
                    .and(CLUSTER_HOST_MAP.REMOVED.isNull())
                .fetchOneInto(ClusterHostMapRecord.class);
    }

    @Override
    public IpAddress getIpAddressForHost(Long hostId) {
        return create()
            .select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(HOST_IP_ADDRESS_MAP)
                    .on(IP_ADDRESS.ID.eq(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID))
                .where(HOST_IP_ADDRESS_MAP.HOST_ID.eq(hostId)
                    .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                    .and(IP_ADDRESS.REMOVED.isNull()))
            .fetchOneInto(IpAddressRecord.class);
    }

    @Override
    public Host findHostByName(Long accountId, String name) {
        return create()
            .select(HOST.fields())
                .from(HOST)
                .where(HOST.NAME.eq(name))
                    .and(HOST.ACCOUNT_ID.eq(accountId))
                    .and(HOST.REMOVED.isNull())
            .fetchOneInto(HostRecord.class);
    }
}
