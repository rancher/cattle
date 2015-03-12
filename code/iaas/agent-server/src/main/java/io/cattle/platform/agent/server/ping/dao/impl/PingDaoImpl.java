package io.cattle.platform.agent.server.ping.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import io.cattle.platform.agent.server.group.AgentGroupManager;
import io.cattle.platform.agent.server.ping.dao.PingDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.impl.DSL;

public class PingDaoImpl extends AbstractJooqDao implements PingDao {

    AgentGroupManager groupManager;

    @Override
    public List<? extends Agent> findAgentsToPing() {
        return create()
                .selectFrom(AGENT)
                .where(
                        AGENT.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(groupCondition()))
                        .fetch();
    }

    protected Condition groupCondition() {
        if ( groupManager.shouldHandleWildcard() ) {
            return DSL.trueCondition();
        }

        Condition condition = DSL.falseCondition();
        Set<Long> supported = groupManager.supportedGroups();

        if ( supported.size() > 0 ) {
            condition = AGENT.AGENT_GROUP_ID.in(supported);
        }

        if ( groupManager.shouldHandleUnassigned() ) {
            condition = condition.or(AGENT.AGENT_GROUP_ID.isNull());
        }

        return condition;
    }

    public AgentGroupManager getGroupManager() {
        return groupManager;
    }

    @Inject
    public void setGroupManager(AgentGroupManager groupManager) {
        this.groupManager = groupManager;
    }
}
