package io.cattle.platform.agent.instance.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AgentInstanceInstanceCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();

        Agent agent = agentInstanceFactory.createAgent(instance);

        return agent == null ? null : new HandlerResult((Object)INSTANCE.AGENT_ID, agent.getId());
    }

}