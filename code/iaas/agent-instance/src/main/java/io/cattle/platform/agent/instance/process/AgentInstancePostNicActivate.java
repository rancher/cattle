package io.cattle.platform.agent.instance.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class AgentInstancePostNicActivate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
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

    @Override
    public int getPriority() {
        return Priority.DEFAULT + 1;
    }
}
