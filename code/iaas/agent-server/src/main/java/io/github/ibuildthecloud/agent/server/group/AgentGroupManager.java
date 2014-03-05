package io.github.ibuildthecloud.agent.server.group;

import java.util.Set;

import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.core.model.AgentGroup;

public interface AgentGroupManager {

    boolean shouldHandle(Agent agent);

    boolean shouldHandle(long agentId);

    boolean shouldHandleGroup(AgentGroup agent);

    boolean shouldHandleGroup(Long agentGroupId);

    boolean shouldHandleWildcard();

    boolean shouldHandleUnassigned();

    Set<Long> supportedGroups();

}
