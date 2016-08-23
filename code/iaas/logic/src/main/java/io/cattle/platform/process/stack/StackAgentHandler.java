package io.cattle.platform.process.stack;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

import javax.inject.Inject;

public class StackAgentHandler extends AgentBasedProcessHandler {

    @Inject
    AgentInstanceDao agentInstanceDao;
    String agentService;
    String stackKind;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Stack env = (Stack)state.getResource();
        if (!stackKind.equals(env.getKind())) {
            return null;
        }

        Long accountId = env.getAccountId();
        List<Long> agentIds = agentInstanceDao.getAgentProvider(agentService, accountId);
        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

    public String getStackKind() {
        return stackKind;
    }

    @Inject
    public void setStackKind(String environmentKind) {
        this.stackKind = environmentKind;
    }

    public String getAgentService() {
        return agentService;
    }

    @Inject
    public void setAgentService(String agentService) {
        this.agentService = agentService;
    }

}