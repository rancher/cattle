package io.github.ibuildthecloud.dstack.agent.server.lock;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

import com.netflix.config.DynamicLongProperty;

public class AgentConnectionManagementLock extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty AGENT_CONNECTION_MANAGEMENT_WAIT_LOCK = ArchaiusUtil.getLong("agent.connection.management.lock.wait.millis");

    public AgentConnectionManagementLock(Agent agent) {
        super("AGENT.CONNECTION.MANAGEMENT." + agent.getId());
    }

    @Override
    public long getWait() {
        return AGENT_CONNECTION_MANAGEMENT_WAIT_LOCK.get();
    }

}