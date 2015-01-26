package io.cattle.platform.core.dao.impl;

import java.util.List;

import static io.cattle.platform.core.model.tables.ClusterHostMapTable.*;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
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

}
