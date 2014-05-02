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
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

public class AgentInstanceApplyItems extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final Logger log = LoggerFactory.getLogger(AgentInstanceApplyItems.class);

    private static final DynamicStringListProperty BASE = ArchaiusUtil.getList("agent.instance.services.base.items");
    private static final DynamicStringListProperty PROCESS_NAMES = ArchaiusUtil.getList("agent.instance.services.processes");

    AgentInstanceManager agentInstanceManager;
    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;
    boolean assignBase = false;

    @Override
    public String[] getProcessNames() {
        List<String> result = PROCESS_NAMES.get();
        return result.toArray(new String[result.size()]);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = getNic(state, process);

        if ( nic == null ) {
            log.error("Failed to find nic for [{}:{}]", objectManager.getType(state.getResource()), state.getResourceId());
            return null;
        }

        Map<NetworkServiceProvider, Instance> agentInstances = agentInstanceManager.getAgentInstances(nic);
        for ( Map.Entry<NetworkServiceProvider, Instance> entry : agentInstances.entrySet() ) {
            assignItems(entry.getKey(), entry.getValue(), nic, state, process);
        }

        return null;
    }

    protected Nic getNic(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        return agentInstanceManager.getNicFromResource(resource);
    }

    protected void assignItems(NetworkServiceProvider provider, Instance agentInstance, Nic nic,
            ProcessState state, ProcessInstance processInstance) {
        Agent agent = objectManager.loadResource(Agent.class, agentInstance.getAgentId());
        if ( agent == null ) {
            return;
        }

        String contextId = getContext(processInstance, nic);

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, contextId);
        if ( request == null ) {
            request = new ConfigUpdateRequest(agent.getId());
            ConfigUpdateRequestUtils.setWaitFor(request);
            assignBaseItems(provider, request, agent, processInstance);
            assignServiceItems(provider, agentInstance, nic, request, agent, state, processInstance);
        }

        if ( request != null ) {
            statusManager.updateConfig(request);
            ConfigUpdateRequestUtils.setRequest(request, state, contextId);
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

            String context = getContext(processInstance, nic) + "." + otherAgent.getId();
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

    public String getContext(ProcessInstance instance, Object obj) {
        return String.format("%s:%s:%s", instance.getName(), objectManager.getType(obj), ObjectUtils.getId(obj));
    }

    protected void assignBaseItems(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent,
            ProcessInstance processInstance) {
        String key = String.format("%s.%s.base.items", processInstance.getName(), provider.getKind());

        if ( ArchaiusUtil.getBoolean(key).get() ) {
            for ( String item : BASE.get() ) {
                request.addItem(item)
                    .withApply(true)
                    .withIncrement(false)
                    .withCheckInSyncOnly(true);
            }
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

    public boolean isAssignBase() {
        return assignBase;
    }

    public void setAssignBase(boolean assignBase) {
        this.assignBase = assignBase;
    }

}
