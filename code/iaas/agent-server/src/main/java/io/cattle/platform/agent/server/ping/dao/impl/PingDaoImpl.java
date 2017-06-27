package io.cattle.platform.agent.server.ping.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.agent.server.ping.dao.PingDao;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

import org.jooq.Condition;
import org.jooq.Configuration;

public class PingDaoImpl extends AbstractJooqDao implements PingDao {

    public PingDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public List<Long> findAgentsToPing() {
        Condition c = AGENT.STATE.eq(CommonStatesConstants.ACTIVE);
        for (String prefix : AgentConstants.AGENT_IGNORE_PREFIXES) {
            c = c.and(AGENT.URI.notLike(prefix + "%"));
        }
        return create()
                .select(AGENT.ID)
                .from(AGENT)
                .where(c)
                .fetch(AGENT.ID);
    }

}
