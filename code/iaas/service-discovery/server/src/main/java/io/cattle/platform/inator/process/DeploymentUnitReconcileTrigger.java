package io.cattle.platform.inator.process;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.inator.InatorLifecycleManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    InatorLifecycleManager lifecycleManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                InstanceConstants.PROCESS_CREATE,
                InstanceConstants.PROCESS_START,
                InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE,
                InstanceConstants.PROCESS_ERROR,
                "volume.*",
                HostConstants.PROCESS_REMOVE,
                HostConstants.PROCESS_ACTIVATE,
                AgentConstants.PROCESS_RECONNECT,
                AgentConstants.PROCESS_DECONNECT,
                AgentConstants.PROCESS_FINISH_RECONNECT,
                AgentConstants.PROCESS_ACTIVATE,
                HealthcheckConstants.PROCESS_UPDATE_HEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_REINITIALIZING
                };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        lifecycleManager.triggerDeploymentUnitUpdate(state.getResource());
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}
