package io.cattle.platform.agent.instance.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class AgentInstanceServicesInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPreListener {

    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        for (ConfigUpdateRequest request : ConfigUpdateRequestUtils.getRequests(jsonMapper, state)) {
            if (ConfigUpdateRequestUtils.shouldWaitFor(request)) {
                statusManager.waitFor(request);
            }
        }

        return null;
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
