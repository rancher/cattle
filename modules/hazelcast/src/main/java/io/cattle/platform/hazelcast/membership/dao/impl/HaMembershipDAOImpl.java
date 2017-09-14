package io.cattle.platform.hazelcast.membership.dao.impl;

import io.cattle.platform.core.model.HaMembership;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.hazelcast.membership.ClusterConfig;
import io.cattle.platform.hazelcast.membership.dao.HaMembershipDAO;
import io.cattle.platform.json.JsonMapper;
import org.jooq.Configuration;

import java.io.IOException;
import java.util.List;

import static io.cattle.platform.core.model.tables.HaMembershipTable.*;

public class HaMembershipDAOImpl extends AbstractJooqDao implements HaMembershipDAO {

    JsonMapper jsonMapper;

    public HaMembershipDAOImpl(Configuration configuration, JsonMapper jsonMapper) {
        super(configuration);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void checkin(String uuid, ClusterConfig config, boolean initial) throws IOException {
        if (initial) {
            create().delete(HA_MEMBERSHIP)
                .where(HA_MEMBERSHIP.UUID.eq(uuid))
                .execute();
        }

        int count = create().update(HA_MEMBERSHIP)
            .set(HA_MEMBERSHIP.HEARTBEAT, System.currentTimeMillis())
            .where(HA_MEMBERSHIP.UUID.eq(uuid))
            .execute();
        if (count == 0) {
            if (!initial) {
                throw new IllegalStateException("Failed to update check-in, registration deleted");
            }
            String content = jsonMapper.writeValueAsString(config);
            create().insertInto(HA_MEMBERSHIP,
                    HA_MEMBERSHIP.UUID, HA_MEMBERSHIP.HEARTBEAT, HA_MEMBERSHIP.CONFIG, HA_MEMBERSHIP.CLUSTERED)
                .values(uuid, System.currentTimeMillis(), content, config.isClustered()).execute();
        }
    }

    @Override
    public List<? extends HaMembership> listMembers() {
        return create().selectFrom(HA_MEMBERSHIP).fetch();
    }

    @Override
    public void delete(HaMembership member) {
        create().delete(HA_MEMBERSHIP)
            .where(HA_MEMBERSHIP.ID.eq(member.getId()))
            .execute();
    }

}
