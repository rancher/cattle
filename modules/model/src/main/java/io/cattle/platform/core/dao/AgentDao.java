package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;

import java.util.List;
import java.util.Map;

public interface AgentDao {

    Map<String, Host> getHosts(long agentId);

    Host getHost(long agentId, String externalId);

    Map<String, StoragePool> getStoragePools(long agentId);

    Agent getHostAgentForDelegate(long agentId);

    String getAgentState(long agentId);

    List<? extends Agent> findAgentsToRemove();

    Instance getInstanceByAgent(Long agentId);

    boolean areAllCredentialsActive(Agent agent);

    Agent findNonRemovedByUri(String uri);

}
