package io.cattle.platform.lb.instance.process;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener {
    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());
        if (!lbInstanceManager.isLbInstance(instance)) {
            return null;
        }
        // set host id on the mapping
        LoadBalancerHostMap hostMap = lbInstanceManager.getLoadBalancerHostMapForInstance(instance);
        if (hostMap.getHostId() == null) {
            hostMap.setHostId(map.getHostId());
            objectManager.persist(hostMap);
        }

        return null;
    }
}
