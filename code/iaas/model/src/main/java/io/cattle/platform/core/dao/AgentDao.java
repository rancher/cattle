package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;

import java.util.Map;

public interface AgentDao {

    Agent findNonRemovedByUri(String uri);

    Map<String, Host> getHosts(long agentId);

    Map<String, StoragePool> getStoragePools(long agentId);

    Map<String, PhysicalHost> getPhysicalHosts(long agentId);

    Agent getHostAgentForDelegate(long agentId);

    Host getHost(Agent agent);

    Instance getInstance(Agent agent);

    String getAgentState(long agentId);

}
