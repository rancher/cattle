package io.cattle.platform.agent.instance.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

public class AgentInstanceHostServicesApplyItems extends AbstractHostApplyItems {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceHostServicesApplyItems.class);

    private static final DynamicStringListProperty PROCESS_NAMES = ArchaiusUtil.getList("host.agent.instance.services.processes");

    AgentInstanceManager agentInstanceManager;

    @Override
    public String[] getProcessNames() {
        List<String> result = PROCESS_NAMES.get();
        return result.toArray(new String[result.size()]);
    }

    @Override
    protected Map<Host, List<NetworkServiceProvider>> getHosts(ProcessState state, ProcessInstance process) {
        Map<Host, List<NetworkServiceProvider>> hosts = new HashMap<Host, List<NetworkServiceProvider>>();

        List<? extends Nic> nics = getNics(state, process);

        if (nics == null) {
            log.error("Failed to find nic for [{}:{}]", objectManager.getType(state.getResource()), state.getResourceId());
            return null;
        }

        for (Nic nic : nics) {
            Instance instance = loadResource(Instance.class, nic.getInstanceId());
            for (Host host : mappedChildren(instance, Host.class)) {
                Map<NetworkServiceProvider, Instance> agentInstances = agentInstanceManager.getAgentInstances(nic);

                List<NetworkServiceProvider> providers = new ArrayList<NetworkServiceProvider>(agentInstances.keySet());

                if (providers.size() > 0) {
                    hosts.put(host, providers);
                }
            }
        }

        return hosts;
    }

    protected List<? extends Nic> getNics(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        return agentInstanceManager.getNicsFromResource(resource);
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

    @Override
    protected String getConfigPrefix() {
        return "host.";
    }

}
