package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerUpdateAll extends AbstractObjectProcessHandler {

    @Inject
    LoadBalancerSetHosts setHosts;

    @Inject
    LoadBalancerSetTargets setTargets;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_UPDATE_ALL };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        setHosts.handle(state, process);
        setTargets.handle(state, process);
        return null;
    }
}
