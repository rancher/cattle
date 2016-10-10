package io.cattle.platform.agent.instance.dao;

import io.cattle.platform.agent.instance.service.NetworkServiceInfo;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;

import java.util.List;
import java.util.Map;

public interface AgentInstanceDao {

    Agent getAgentByUri(String uri);

    Instance getInstanceByAgent(Agent agent);

    List<? extends NetworkServiceProvider> getProviders(Long networkId);

    Instance getAgentInstance(NetworkServiceProvider provider, Nic nic);

    Instance createInstanceForProvider(NetworkServiceProvider provider, Map<String, Object> properties);

    List<? extends Agent> getAgents(NetworkServiceProvider provider);

    List<? extends Credential> getActivateCredentials(Agent agent);

    NetworkServiceInfo getNetworkServiceInfo(long instance, String serviceKind);

    void populateNicAndIp(NetworkServiceInfo service);

    List<Long> getAgentProvider(String providedServiceLabel, long accountId);

    List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long accountId);
}
