package io.github.ibuildthecloud.dstack.process.agent;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateRequest;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.engine.handler.AbstractProcessLogic;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.util.DataAccessor;

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
       return new String[] { "agent.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();
        if ( ! agent.getManagedConfig() ) {
            return null;
        }

        DataAccessor data = DataAccessor
                .fromMap(state.getData())
                .withScope(ConfigUpdateRequest.class)
                .withKey(REQUEST_KEY);

        ConfigUpdateRequest request = data.as(jsonMapper, ConfigUpdateRequest.class);

        switch (state.getPhase()) {
        case PRE_LISTENERS:
            request = before(request, agent);
            break;
        case POST_LISTENERS:
            after(request, agent);
            break;
        default:
        }

        data.set(request);

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
