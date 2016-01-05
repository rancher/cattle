package io.cattle.platform.agent.instance.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHostApplyItems extends AbstractApplyItems implements ProcessPostListener {

    private static final Logger log = LoggerFactory.getLogger(AbstractHostApplyItems.class);

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Map<Host, List<NetworkServiceProvider>> hosts = getHosts(state, process);

        if (hosts == null) {
            log.error("Failed to find hosts for [{}:{}]", objectManager.getType(state.getResource()), state.getResourceId());
            return null;
        }

        for (Map.Entry<Host, List<NetworkServiceProvider>> entry : hosts.entrySet()) {
            Host host = entry.getKey();
            Agent agent = loadResource(Agent.class, host.getAgentId());
            if (agent == null) {
                continue;
            }

            for (NetworkServiceProvider provider : entry.getValue()) {
                assignItems(provider, agent, host, state, process, true);
            }
        }

        return null;
    }

    protected abstract Map<Host, List<NetworkServiceProvider>> getHosts(ProcessState state, ProcessInstance process);

    @Override
    protected List<? extends Agent> getOtherAgents(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessState state,
            ProcessInstance processInstance) {
        return Collections.emptyList();
    }

}
