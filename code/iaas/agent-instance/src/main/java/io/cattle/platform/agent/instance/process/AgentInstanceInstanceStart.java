package io.cattle.platform.agent.instance.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public class AgentInstanceInstanceStart extends AbstractObjectProcessLogic implements ProcessPreListener {

    private static final DynamicStringListProperty APPLY = ArchaiusUtil.getList("agent.instance.start.items.apply");
    private static final DynamicStringListProperty INCREMENT = ArchaiusUtil.getList("agent.instance.start.items.increment");

    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ConfigUpdateRequest request = getRequest(state, process);

        if ( request == null ) {
            return null;
        }

        statusManager.updateConfig(request);

        ConfigUpdateRequestUtils.setRequest(request, state, this);

        return null;
    }

    protected ConfigUpdateRequest getRequest(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        Agent agent = loadResource(Agent.class, instance.getAgentId());

        if ( agent == null ) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, this);

        if ( request == null ) {
            request = new ConfigUpdateRequest(agent.getId());

            for ( String item : APPLY.get() ) {
                request.addItem(item)
                    .withApply(true)
                    .withIncrement(false)
                    .withCheckInSync(true);
            }

            for ( String item : INCREMENT.get() ) {
                request.addItem(item)
                    .withApply(true)
                    .withIncrement(true)
                    .withCheckInSync(false);
            }
        }

        return request;
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
