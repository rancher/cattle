package io.cattle.platform.agent.instance.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.Map;

import javax.inject.Inject;

public class AgentInstanceServicesInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPreListener {

    AgentInstanceManager agentInstanceManager;
    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Long instanceId = ((InstanceHostMap)state.getResource()).getInstanceId();
        Instance instance = objectManager.loadResource(Instance.class, instanceId);

        for ( Nic nic : objectManager.children(instance, Nic.class) ) {
            Map<NetworkServiceProvider, Instance> agentInstances = agentInstanceManager.getAgentInstances(nic);
            for ( Map.Entry<NetworkServiceProvider, Instance> entry : agentInstances.entrySet() ) {
                waitFor(entry.getKey(), entry.getValue(), nic, state);
            }
        }


        return null;
    }

    protected void waitFor(NetworkServiceProvider provider, Instance agentInstance, Nic nic, ProcessState state) {
        Agent agent = objectManager.loadResource(Agent.class, agentInstance.getAgentId());
        if ( agent == null ) {
            return;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, AgentInstanceServicesNicActivate.getContext(nic));
        if ( request != null ) {
            statusManager.waitFor(request);
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
