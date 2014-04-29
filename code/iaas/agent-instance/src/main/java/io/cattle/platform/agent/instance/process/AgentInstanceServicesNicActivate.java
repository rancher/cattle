package io.cattle.platform.agent.instance.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public class AgentInstanceServicesNicActivate extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final DynamicStringListProperty BASE = ArchaiusUtil.getList("agent.instance.services.base.items");

    public static final String ITEMS_CONTEXT = "agentInstanceServices";

    AgentInstanceManager agentInstanceManager;
    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        Map<NetworkServiceProvider, Instance> agentInstances = agentInstanceManager.getAgentInstances(nic);
        for ( Map.Entry<NetworkServiceProvider, Instance> entry : agentInstances.entrySet() ) {
            assignItems(entry.getKey(), entry.getValue(), nic, state, process);
        }

        return null;
    }

    protected void assignItems(NetworkServiceProvider provider, Instance agentInstance, Nic nic,
            ProcessState state, ProcessInstance processInstance) {
        Agent agent = objectManager.loadResource(Agent.class, agentInstance.getAgentId());
        if ( agent == null ) {
            return;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, getContext(nic));
        if ( request == null ) {
            request = new ConfigUpdateRequest(agent.getId());
            assignBaseItems(request, agent);
            assignServiceItems(provider, agentInstance, nic, request, agent, state, processInstance);
        }

        if ( request != null ) {
            statusManager.updateConfig(request);
            ConfigUpdateRequestUtils.setRequest(request, state, getContext(nic));
        }
    }

    protected void assignServiceItems(NetworkServiceProvider provider, Instance agentInstance, Nic nic,
            ConfigUpdateRequest request, Agent agent, ProcessState state, ProcessInstance processInstance) {
        Set<String> apply = new HashSet<String>();
        Set<String> increment = new HashSet<String>();
        String prefix = String.format("%s.%s", processInstance.getName(), provider.getKind());

        for ( NetworkService service : objectManager.children(provider, NetworkService.class) ) {
            apply.addAll(ArchaiusUtil.getList(String.format("%s.%s.apply", prefix, service.getKind())).get());
            increment.addAll(ArchaiusUtil.getList(String.format("%s.%s.increment", prefix, service.getKind())).get());
        }

        setItems(request, apply, increment);

        for ( Agent otherAgent : agentInstanceManager.getAgents(provider) ) {
            if ( otherAgent.getId().equals(agent.getId()) ) {
                continue;
            }

            String context = getContext(nic) + "." + otherAgent.getId();
            ConfigUpdateRequest otherRequest = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, context);
            if ( otherRequest == null ) {
                otherRequest = new ConfigUpdateRequest(otherAgent.getId());
                setItems(otherRequest, apply, increment);

                statusManager.updateConfig(otherRequest);
                ConfigUpdateRequestUtils.setRequest(otherRequest, state, context);
            }
        }
    }

    protected void setItems(ConfigUpdateRequest request, Set<String> apply, Set<String> increment) {
        for ( String item : apply ) {
            request.addItem(item)
                    .withApply(true)
                    .withIncrement(false)
                    .withCheckInSyncOnly(true);
        }

        for ( String item : increment ) {
            request.addItem(item)
                    .withApply(true)
                    .withIncrement(true)
                    .withCheckInSyncOnly(false);
        }
    }

    public static String getContext(Nic nic) {
        return String.format("%s:nic:%s", ITEMS_CONTEXT, nic.getId());
    }

    protected void assignBaseItems(ConfigUpdateRequest request, Agent agent) {
        for ( String item : BASE.get() ) {
            request.addItem(item)
                .withApply(true)
                .withIncrement(false)
                .withCheckInSyncOnly(true);
        }
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ConfigItemStatusManager getStatusManager() {
        return statusManager;
    }

    @Inject
    public void setStatusManager(ConfigItemStatusManager statusManager) {
        this.statusManager = statusManager;
    }

}
