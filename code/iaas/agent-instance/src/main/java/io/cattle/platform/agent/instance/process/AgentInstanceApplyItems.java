package io.cattle.platform.agent.instance.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

public class AgentInstanceApplyItems extends AbstractApplyItems implements ProcessPostListener {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceApplyItems.class);

    private static final DynamicStringListProperty PROCESS_NAMES = ArchaiusUtil.getList("agent.instance.services.processes");

    AgentInstanceManager agentInstanceManager;

    @Override
    public String[] getProcessNames() {
        List<String> result = PROCESS_NAMES.get();
        return result.toArray(new String[result.size()]);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<? extends Nic> nics = getNics(state, process);

        if (nics == null) {
            log.debug("Failed to find nic for [{}:{}]", objectManager.getType(state.getResource()),
                    state.getResourceId());
            return null;
        }

        for (Nic nic : nics) {
            Map<NetworkServiceProvider, Instance> agentInstances = agentInstanceManager.getAgentInstances(nic, true);
            for (Map.Entry<NetworkServiceProvider, Instance> entry : agentInstances.entrySet()) {
                Agent agent = objectManager.loadResource(Agent.class, entry.getValue().getAgentId());
                log.info("Processing agent [{}] nic [{}]", agent.getId(), nic.getId());

                /* Don't wait if this nic is a nic to the agent instance */
                assignItems(entry.getKey(), agent, nic, state, process, !entry.getValue().getId().equals(nic.getInstanceId()));
                log.info("Done processing agent [{}] nic [{}]", agent.getId(), nic.getId());
            }
        }

        return null;
    }

    protected List<? extends Nic> getNics(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        return agentInstanceManager.getNicsFromResource(resource);
    }

    @Override
    protected List<? extends Agent> getOtherAgents(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessState state,
            ProcessInstance processInstance) {
        return agentInstanceManager.getAgents(provider);
    }

    @Override
    protected String getConfigPrefix() {
        return "";
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

}
