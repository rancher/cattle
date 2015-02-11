package io.cattle.platform.lb.instance.process;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerAddHostPostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_HOST_MAP_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancerHostMap map = (LoadBalancerHostMap) state.getResource();
        LoadBalancer lb = loadResource(LoadBalancer.class, map.getLoadBalancerId());
        long hostId = map.getHostId();

        lbInstanceManager.createLoadBalancerInstances(lb, hostId);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
