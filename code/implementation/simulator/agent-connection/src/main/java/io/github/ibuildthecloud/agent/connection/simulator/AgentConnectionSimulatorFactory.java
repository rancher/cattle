package io.github.ibuildthecloud.agent.connection.simulator;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.dstack.core.model.Agent;

public class AgentConnectionSimulatorFactory implements AgentConnectionFactory {

    List<AgentSimulatorEventProcessor> processors;

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();

        if ( uri != null && uri.startsWith("sim://") ) {
            return new AgentConnectionSimulator(agent, processors);
        }

        return null;
    }

    public List<AgentSimulatorEventProcessor> getProcessors() {
        return processors;
    }

    @Inject
    public void setProcessors(List<AgentSimulatorEventProcessor> processors) {
        this.processors = processors;
    }

}
