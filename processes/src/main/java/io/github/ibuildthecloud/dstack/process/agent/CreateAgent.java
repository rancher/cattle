package io.github.ibuildthecloud.dstack.process.agent;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.command.PingCommand;
import io.github.ibuildthecloud.dstack.engine.handler.AbstractDefaultProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;

@Named
public class CreateAgent extends AbstractDefaultProcessHandler {

    AgentLocator agentLocator;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        RemoteAgent agent = agentLocator.lookupAgent(state.getResource());
        if ( agent != null ) {
            agent.callSync(new PingCommand());
        }

        return HandlerResult.EMPTY_RESULT;
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

}
