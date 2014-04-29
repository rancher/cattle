package io.cattle.platform.agent.instance.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class AgentInstanceNicActivate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    AgentInstanceManager agentInstanceManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        agentInstanceManager.getAgentInstances((Nic)state.getResource());

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

}