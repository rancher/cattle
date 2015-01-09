package io.cattle.platform.agent.connection.simulator;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.agent.server.connection.AgentConnectionFactory;
import io.cattle.platform.core.model.Agent;

public class AgentConnectionSimulatorFactory implements AgentConnectionFactory {

    List<AgentSimulatorEventProcessor> processors;

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();

        if (uri != null && uri.startsWith("sim://")) {
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
