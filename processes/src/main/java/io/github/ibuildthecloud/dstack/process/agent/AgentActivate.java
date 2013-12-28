package io.github.ibuildthecloud.dstack.process.agent;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.command.PingCommand;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AgentActivate extends AbstractDefaultProcessHandler {

    AgentLocator agentLocator;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();

        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);
        remoteAgent.callSync(new PingCommand());

        return new HandlerResult();
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }


}
