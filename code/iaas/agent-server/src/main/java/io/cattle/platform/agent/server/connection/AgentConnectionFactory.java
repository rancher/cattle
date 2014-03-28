package io.cattle.platform.agent.server.connection;

import io.cattle.platform.core.model.Agent;

import java.io.IOException;

public interface AgentConnectionFactory {

    AgentConnection createConnection(Agent agent) throws IOException;

}
