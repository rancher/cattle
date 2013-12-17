package io.github.ibuildthecloud.agent.server.lock;

import com.netflix.config.DynamicLongProperty;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

public class AgentConnectionManagementLock extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty AGENT_CONNECTION_MANAGEMENT_WAIT_LOCK = ArchaiusUtil.getLongProperty("agent.connection.management.lock.wait.millis");

    public AgentConnectionManagementLock(Agent agent) {
        super("AGENT.CONNECTION.MANAGEMENT." + agent.getId());
    }

    @Override
    public long getWait() {
        return AGENT_CONNECTION_MANAGEMENT_WAIT_LOCK.get();
    }

}