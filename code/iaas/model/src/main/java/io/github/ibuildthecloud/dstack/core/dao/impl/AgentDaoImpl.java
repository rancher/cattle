package io.github.ibuildthecloud.dstack.core.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.AgentTable.*;
import io.github.ibuildthecloud.dstack.core.dao.AgentDao;
import io.github.ibuildthecloud.dstack.core.model.Agent;

public class AgentDaoImpl extends AbstractCoreDao implements AgentDao {

    @Override
    public Agent findNonRemovedByUri(String uri) {
        return create()
                .selectFrom(AGENT)
                .where(
                        AGENT.URI.eq(uri)
                        .and(AGENT.REMOVED.isNull()))
                .fetchOne();
    }

}
