package io.cattle.platform.hazelcast.membership.dao.impl;

import static io.cattle.platform.core.model.tables.ClusterMembershipTable.*;

import io.cattle.platform.core.model.ClusterMembership;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.hazelcast.membership.ClusterConfig;
import io.cattle.platform.hazelcast.membership.dao.ClusterMembershipDAO;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class ClusterMembershipDAOImpl extends AbstractJooqDao implements ClusterMembershipDAO {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public void checkin(String uuid, ClusterConfig config, boolean initial) throws IOException {
        if (initial) {
            create().delete(CLUSTER_MEMBERSHIP)
                .where(CLUSTER_MEMBERSHIP.UUID.eq(uuid))
                .execute();
        }

        int count = create().update(CLUSTER_MEMBERSHIP)
            .set(CLUSTER_MEMBERSHIP.HEARTBEAT, System.currentTimeMillis())
            .where(CLUSTER_MEMBERSHIP.UUID.eq(uuid))
            .execute();
        if (count == 0) {
            if (!initial) {
                throw new IllegalStateException("Failed to update check-in, registration deleted");
            }
            String content = jsonMapper.writeValueAsString(config);
            create().insertInto(CLUSTER_MEMBERSHIP,
                    CLUSTER_MEMBERSHIP.UUID, CLUSTER_MEMBERSHIP.HEARTBEAT, CLUSTER_MEMBERSHIP.CONFIG, CLUSTER_MEMBERSHIP.CLUSTERED)
                .values(uuid, System.currentTimeMillis(), content, config.isClustered()).execute();
        }
    }

    @Override
    public List<? extends ClusterMembership> listMembers() {
        return create().selectFrom(CLUSTER_MEMBERSHIP).fetch();
    }

    @Override
    public void delete(ClusterMembership member) {
        create().delete(CLUSTER_MEMBERSHIP)
            .where(CLUSTER_MEMBERSHIP.ID.eq(member.getId()))
            .execute();
    }

}
