package io.cattle.platform.process.agent;

import io.cattle.platform.agent.impl.AgentLocatorImpl;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.util.ProcessUtils;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;

@Named
public class AgentResourceRemove extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Override
    public String[] getProcessNames() {
        List<String> result = new ArrayList<>();
        for (String i : AgentUtils.AGENT_RESOURCES.get()) {
            result.add(String.format("%s.remove", i).toLowerCase());
        }

        return result.toArray(new String[result.size()]);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();

        Long agentId = AgentLocatorImpl.getAgentId(resource);
        Agent agent = objectManager.loadResource(Agent.class, agentId);

        if (agent == null || agent.getRemoved() != null) {
            return null;
        }

        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE,
                    agent, ProcessUtils.chainInData(state.getData(), AgentConstants.PROCESS_DEACTIVATE,
                            AgentConstants.PROCESS_REMOVE));
        } catch (ProcessCancelException e) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, agent, state.getData());
        }

        return null;
    }
}
