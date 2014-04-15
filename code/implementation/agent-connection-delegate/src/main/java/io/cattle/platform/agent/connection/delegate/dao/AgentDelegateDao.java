package io.cattle.platform.agent.connection.delegate.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;

public interface AgentDelegateDao {

    Host getHost(Agent agent);

    Instance getInstance(Agent agent);

}