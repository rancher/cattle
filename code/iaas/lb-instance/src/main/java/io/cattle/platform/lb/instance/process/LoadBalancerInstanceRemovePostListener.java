package io.cattle.platform.lb.instance.process;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerInstanceRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (!lbInstanceManager.isLbInstance(instance)) {
            return null;
        }

        Agent lbAgent = objectManager.loadResource(Agent.class, instance.getAgentId());

        // try to remove first
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, lbAgent,
                    null);
        } catch (ProcessCancelException e) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE,
                    lbAgent,
                    ProcessUtils.chainInData(new HashMap<String, Object>(), AgentConstants.PROCESS_DEACTIVATE,
                            AgentConstants.PROCESS_REMOVE));
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
