package io.cattle.platform.process.agent;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.AbstractProcessLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringListProperty;

@Named
public class AgentScriptsApply extends AbstractProcessLogic implements ProcessPreListener, ProcessPostListener {

    private static final DynamicStringListProperty ITEMS = ArchaiusUtil.getList("agent.config.items");
    public static final String REQUEST_KEY = "agentScripts";

    ConfigItemStatusManager statusManager;
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
       return new String[] { AgentConstants.PROCESS_ACTIVATE, AgentConstants.PROCESS_RECONNECT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();
        if ( ! agent.getManagedConfig() ) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, this);

        switch (state.getPhase()) {
        case PRE_LISTENERS:
            request = before(request, agent);
            break;
        case POST_LISTENERS:
            after(request, agent);
            break;
        default:
        }

        ConfigUpdateRequestUtils.setRequest(request, state, this);

        return null;
    }

    protected ConfigUpdateRequest before(ConfigUpdateRequest request, Agent agent){
        if ( request == null ) {
            request = new ConfigUpdateRequest(agent.getId());
            for ( String item : ITEMS.get() ) {
                request.addItem(item)
                    .withIncrement(false)
                    .setCheckInSync(true);
            }
        }

        statusManager.updateConfig(request);

        return request;
    }

    protected void after(ConfigUpdateRequest request, Agent agent){
        if ( request == null ) {
            return;
        }

        statusManager.waitFor(request);
    }

    public ConfigItemStatusManager getStatusManager() {
        return statusManager;
    }

    @Inject
    public void setStatusManager(ConfigItemStatusManager statusManager) {
        this.statusManager = statusManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
