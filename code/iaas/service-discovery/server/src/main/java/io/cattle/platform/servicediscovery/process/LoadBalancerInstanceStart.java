package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class LoadBalancerInstanceStart extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ConfigUpdateRequest request = getRequest(state, process);

        if (request == null) {
            return null;
        }

        statusManager.updateConfig(request);

        ConfigUpdateRequestUtils.setRequest(request, state, this);

        return null;
    }

    protected ConfigUpdateRequest getRequest(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        Agent agent = loadResource(Agent.class, instance.getAgentId());

        if (agent == null || instance.getSystemContainer() == null
                || !instance.getSystemContainer().equalsIgnoreCase(InstanceConstants.SYSTEM_CONTAINER_LB_AGENT)) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, this);

        if (request == null) {
            request = ConfigUpdateRequest.forResource(Agent.class, agent.getId());

            request.addItem(LoadBalancerServiceUpdateConfig.CONFIG_ITEM_NAME).withApply(true).withIncrement(false)
                    .withCheckInSyncOnly(true);

            request.addItem(LoadBalancerServiceUpdateConfig.CONFIG_ITEM_NAME).withApply(true).withIncrement(true)
                    .withCheckInSyncOnly(false);
        }

        return request;
    }
}
