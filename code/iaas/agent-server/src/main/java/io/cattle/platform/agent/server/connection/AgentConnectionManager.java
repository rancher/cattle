package io.cattle.platform.agent.server.connection;

import io.cattle.platform.core.model.Agent;

public interface AgentConnectionManager {

    void closeConnection(Agent agent);

    AgentConnection getConnection(Agent agent);

}
