package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class LoadBalancerServiceUpdateConfigPostListener extends AbstractObjectProcessLogic implements
        ProcessPreListener, Priority {

    @Inject
    LoadBalancerServiceUpdateConfig updateConfig;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                InstanceConstants.PROCESS_STOP
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        updateConfig.updateLoadBalancerServices(state, process, true);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
