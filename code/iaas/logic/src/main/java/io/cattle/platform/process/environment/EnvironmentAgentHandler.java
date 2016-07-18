package io.cattle.platform.process.environment;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

import javax.inject.Inject;

public class EnvironmentAgentHandler extends AgentBasedProcessHandler {

    @Inject
    AgentInstanceDao agentInstanceDao;
    String agentService;
    String environmentKind;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Environment env = (Environment)state.getResource();
        if (!environmentKind.equals(env.getKind())) {
            return null;
        }

        Long accountId = env.getAccountId();
        List<Long> agentIds = agentInstanceDao.getAgentProvider(agentService, accountId);
        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

    public String getEnvironmentKind() {
        return environmentKind;
    }

    @Inject
    public void setEnvironmentKind(String environmentKind) {
        this.environmentKind = environmentKind;
    }

    public String getAgentService() {
        return agentService;
    }

    @Inject
    public void setAgentService(String agentService) {
        this.agentService = agentService;
    }

}