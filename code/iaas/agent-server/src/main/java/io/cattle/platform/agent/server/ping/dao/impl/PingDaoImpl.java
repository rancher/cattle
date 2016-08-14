package io.cattle.platform.agent.server.ping.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.agent.server.ping.dao.PingDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class PingDaoImpl extends AbstractJooqDao implements PingDao {

    @Override
    public List<? extends Agent> findAgentsToPing() {
        return create()
                .selectFrom(AGENT)
                .where(
                        AGENT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(AGENT.URI.notLike("delegate://%"))
                        .and(AGENT.URI.notLike("event:///instanceId%")))
                        .fetch();
    }

}
