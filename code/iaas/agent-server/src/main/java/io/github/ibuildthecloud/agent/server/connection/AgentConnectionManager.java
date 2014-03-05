package io.github.ibuildthecloud.agent.server.connection;

import io.github.ibuildthecloud.dstack.core.model.Agent;

public interface AgentConnectionManager {

    void closeConnection(Agent agent);

    AgentConnection getConnection(Agent agent);

}
