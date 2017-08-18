package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.Register;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;

import java.util.List;
import java.util.Map;

public interface AgentDao {

    Map<String, Host> getHosts(Agent agent);

    Host getHost(Agent agent, String externalId);

    Map<String, StoragePool> getStoragePools(long agentId);

    Agent getHostAgentForDelegate(long agentId);

    String getAgentState(long agentId);

    List<? extends Agent> findAgentsToRemove();

    Instance getInstanceByAgent(Long agentId);

    boolean areAllCredentialsActive(Agent agent);

    Agent findNonRemovedByUri(String uri);

    List<? extends Agent> findAgentsToPing();

    Agent findAgentByExternalId(String externalId, long clusterId);

    Credential findAgentCredentailByExternalId(String externalId, long clusterId);

    Agent createAgentForRegistration(Register register, long clusterId);
}
