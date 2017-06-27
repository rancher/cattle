package io.cattle.platform.process.agent;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;

import com.netflix.config.DynamicStringListProperty;

public class AgentScriptsApply implements ProcessHandler {

    private static final DynamicStringListProperty ITEMS = ArchaiusUtil.getList("agent.config.items");

    ConfigItemStatusManager statusManager;
    JsonMapper jsonMapper;

    public AgentScriptsApply(ConfigItemStatusManager statusManager, JsonMapper jsonMapper) {
        super();
        this.statusManager = statusManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent) state.getResource();
        if (!agent.getManagedConfig()) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, this);

        request = before(request, agent);

        ConfigUpdateRequestUtils.setRequest(request, state, this);

        return null;
    }

    protected ConfigUpdateRequest before(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = ConfigUpdateRequest.forResource(Agent.class, agent.getId());
            for (String item : ITEMS.get()) {
                request.addItem(item).withIncrement(false).setCheckInSyncOnly(true);
            }
        }

        statusManager.updateConfig(request);

        return request;
    }

}