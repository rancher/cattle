package io.cattle.platform.process.stack;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.cache.EnvironmentResourceManager;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

public abstract class StackAgentHandler extends AgentBasedProcessHandler {

    EnvironmentResourceManager envResourceManager;
    protected String agentService;
    protected String stackKind;

    public StackAgentHandler(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Stack env = (Stack)state.getResource();
        if (!stackKind.equals(env.getKind())) {
            return null;
        }

        Long accountId = env.getAccountId();
        List<Long> agentIds = envResourceManager.getAgentProvider(agentService, accountId);
        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

    @Override
    protected void preProcessEvent(EventVO<?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);

        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        data.put("environment", data.get("stack"));
    }

}