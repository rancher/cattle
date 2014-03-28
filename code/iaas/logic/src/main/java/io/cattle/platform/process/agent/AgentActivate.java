package io.cattle.platform.process.agent;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

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
