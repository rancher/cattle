package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;

import java.util.List;
import java.util.Map;

public interface AgentDao {

    Agent findNonRemovedByUri(String uri);

    Map<String, Host> getHosts(long agentId);

    Map<String, StoragePool> getStoragePools(long agentId);

    Map<String, PhysicalHost> getPhysicalHosts(long agentId);

    Agent getHostAgentForDelegate(long agentId);

    String getAgentState(long agentId);

    List<? extends Agent> findAgentsToRemove();

}
