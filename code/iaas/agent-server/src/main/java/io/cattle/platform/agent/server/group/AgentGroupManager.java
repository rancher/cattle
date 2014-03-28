package io.cattle.platform.agent.server.group;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.AgentGroup;

import java.util.Set;

public interface AgentGroupManager {

    boolean shouldHandle(Agent agent);

    boolean shouldHandle(long agentId);

    boolean shouldHandleGroup(AgentGroup agent);

    boolean shouldHandleGroup(Long agentGroupId);

    boolean shouldHandleWildcard();

    boolean shouldHandleUnassigned();

    Set<Long> supportedGroups();

}
