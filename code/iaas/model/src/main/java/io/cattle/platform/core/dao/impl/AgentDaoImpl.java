package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;

public class AgentDaoImpl extends AbstractCoreDao implements AgentDao {

    @Override
    public Agent findNonRemovedByUri(String uri) {
        return create().selectFrom(AGENT).where(AGENT.URI.eq(uri).and(AGENT.REMOVED.isNull())).fetchOne();
    }

}
