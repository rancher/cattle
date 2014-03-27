package io.github.ibuildthecloud.dstack.agent.server.connection;

import io.github.ibuildthecloud.dstack.core.model.Agent;

import java.io.IOException;

public interface AgentConnectionFactory {

    AgentConnection createConnection(Agent agent) throws IOException;

}
