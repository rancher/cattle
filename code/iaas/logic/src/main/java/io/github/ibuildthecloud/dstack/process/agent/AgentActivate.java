package io.github.ibuildthecloud.dstack.process.agent;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.agent.util.AgentUtils;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.constants.AgentConstants;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.eventing.EventCallOptions;
import io.github.ibuildthecloud.dstack.framework.event.Ping;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

@Named
public class AgentActivate extends AbstractDefaultProcessHandler {

    private static final DynamicIntProperty PING_RETRY = ArchaiusUtil.getInt("agent.activate.ping.retries");
    private static final DynamicLongProperty PING_TIMEOUT = ArchaiusUtil.getLong("agent.activate.ping.timeout");

    AgentLocator agentLocator;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();

        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);
        remoteAgent.callSync(AgentUtils.newPing(agent)
                .withOption(Ping.STATS, true)
                .withOption(Ping.RESOURCES, true),
                new EventCallOptions(PING_RETRY.get(), PING_TIMEOUT.get()));

        return new HandlerResult();
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { AgentConstants.PROCESS_ACTIVATE, AgentConstants.PROCESS_RECONNECT };
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }


}
