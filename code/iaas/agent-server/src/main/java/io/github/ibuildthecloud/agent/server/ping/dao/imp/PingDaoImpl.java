package io.github.ibuildthecloud.agent.server.ping.dao.imp;

import static io.github.ibuildthecloud.dstack.core.model.tables.AgentTable.*;
import io.github.ibuildthecloud.agent.server.group.AgentGroupManager;
import io.github.ibuildthecloud.agent.server.ping.dao.PingDao;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;

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
