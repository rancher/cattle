package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class LoadBalancerUpdate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE,
                LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        // TODO Add logic for ha proxy config update here as well as updating the config version
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
