package io.cattle.platform.agent.instance.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public abstract class AbstractApplyItems extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final DynamicStringListProperty BASE = ArchaiusUtil.getList("agent.instance.services.base.items");

    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;
    boolean assignBase = true;

    protected void assignItems(NetworkServiceProvider provider, Agent agent, Object owner, ProcessState state, ProcessInstance processInstance) {
        if (agent == null) {
            return;
        }

        String contextId = getContext(processInstance, owner);

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, contextId);
        if (request == null) {
            request = ConfigUpdateRequest.forResource(Agent.class, agent.getId());
            ConfigUpdateRequestUtils.setWaitFor(request);
            if (assignBase) {
                assignBaseItems(provider, request, agent, processInstance);
            }
            assignServiceItems(provider, request, agent, state, processInstance);
        }

        if (request != null) {
            statusManager.updateConfig(request);
            ConfigUpdateRequestUtils.setRequest(request, state, contextId);
        }
    }

    protected abstract String getConfigPrefix();

    protected void assignServiceItems(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessState state,
            ProcessInstance processInstance) {
        Set<String> apply = new HashSet<String>();
        Set<String> increment = new HashSet<String>();
        String prefix = String.format("%s%s.%s", getConfigPrefix(), processInstance.getName(), provider.getKind());

        for (NetworkService service : objectManager.children(provider, NetworkService.class)) {
            apply.addAll(ArchaiusUtil.getList(String.format("%s.%s.apply", prefix, service.getKind())).get());
            increment.addAll(ArchaiusUtil.getList(String.format("%s.%s.increment", prefix, service.getKind())).get());
        }

        setItems(request, apply, increment);

        for (Agent otherAgent : getOtherAgents(provider, request, agent, state, processInstance)) {
            if (otherAgent.getId().equals(agent.getId())) {
                continue;
            }

            String context = getContext(processInstance, otherAgent);
            ConfigUpdateRequest otherRequest = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, context);
            if (otherRequest == null) {
                otherRequest = ConfigUpdateRequest.forResource(Agent.class, otherAgent.getId());
                setItems(otherRequest, apply, increment);

                statusManager.updateConfig(otherRequest);
                ConfigUpdateRequestUtils.setRequest(otherRequest, state, context);
            }
        }
    }

    protected abstract List<? extends Agent> getOtherAgents(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessState state,
            ProcessInstance processInstance);

    protected void setItems(ConfigUpdateRequest request, Set<String> apply, Set<String> increment) {
        for (String item : apply) {
            request.addItem(item).withApply(true).withIncrement(false).withCheckInSyncOnly(true);
        }

        for (String item : increment) {
            request.addItem(item).withApply(true).withIncrement(true).withCheckInSyncOnly(false);
        }
    }

    public String getContext(ProcessInstance instance, Object obj) {
        return String.format("%s:%s:%s", instance.getName(), objectManager.getType(obj), ObjectUtils.getId(obj));
    }

    protected void assignBaseItems(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessInstance processInstance) {
        String key = String.format("%s%s.%s.base.items", getConfigPrefix(), processInstance.getName(), provider.getKind());

        if (ArchaiusUtil.getBoolean(key).get()) {
            for (String item : BASE.get()) {
                request.addItem(item).withApply(true).withIncrement(false).withCheckInSyncOnly(true);
            }
        }
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
