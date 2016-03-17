package io.cattle.platform.docker.process.environment;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.process.util.DockerConstants;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import java.util.List;

import javax.inject.Inject;

public class DockerComposeEnvironmentHandler extends AgentBasedProcessHandler implements ProcessPostListener {

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Environment env = (Environment)state.getResource();
        if (!DockerConstants.TYPE_COMPOSE_PROJECT.equals(env.getKind())) {
            return null;
        }

        Long accountId = env.getAccountId();
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_COMPOSE_PROVIDER, accountId);
        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

}