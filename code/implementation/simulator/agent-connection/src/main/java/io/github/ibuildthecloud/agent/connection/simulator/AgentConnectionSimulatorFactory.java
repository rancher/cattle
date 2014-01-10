package io.github.ibuildthecloud.agent.connection.simulator;

import java.io.IOException;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.dstack.core.model.Agent;

public class AgentConnectionSimulatorFactory implements AgentConnectionFactory {

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();

        if ( uri != null && uri.startsWith("sim://") ) {
            return new AgentConnectionSimulator(agent);
        }

        return null;
    }

}
