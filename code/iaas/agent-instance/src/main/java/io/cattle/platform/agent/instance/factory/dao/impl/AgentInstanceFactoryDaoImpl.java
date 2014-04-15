package io.cattle.platform.agent.instance.factory.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.agent.instance.factory.dao.AgentInstanceFactoryDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class AgentInstanceFactoryDaoImpl extends AbstractJooqDao implements AgentInstanceFactoryDao {

    @Override
    public Agent getAgentByUri(String uri) {
        return create()
                .selectFrom(AGENT)
                .where(AGENT.URI.eq(uri)
                        .and(AGENT.REMOVED.isNull()))
                .fetchAny();
    }

    @Override
    public Instance getInstanceByAgent(Agent agent) {
        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agent.getId())
                        .and(INSTANCE.REMOVED.isNull()))
                .fetchAny();
    }

}