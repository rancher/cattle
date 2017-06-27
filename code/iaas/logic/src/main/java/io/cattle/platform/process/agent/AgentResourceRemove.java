package io.cattle.platform.process.agent;

import io.cattle.platform.agent.impl.AgentLocatorImpl;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

public class AgentResourceRemove implements ProcessHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public AgentResourceRemove(ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();

        Long agentId = AgentLocatorImpl.getAgentId(resource);
        Agent agent = objectManager.loadResource(Agent.class, agentId);

        if (agent == null || agent.getRemoved() != null) {
            return null;
        }

        processManager.deactivateThenRemove(resource, null);

        return null;
    }

}
