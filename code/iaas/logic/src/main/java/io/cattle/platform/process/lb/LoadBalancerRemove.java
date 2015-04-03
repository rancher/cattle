package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerRemove extends AbstractObjectProcessHandler {

    LoadBalancerTargetDao lbTargetDao;
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();

        // remove target mappings
        List<? extends LoadBalancerTarget> targets = lbTargetDao.listByLbIdToRemove(lb.getId());

        for (LoadBalancerTarget target : targets) {
            getObjectProcessManager().scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE,
                    target, null);
        }

        // remove host mappings
        for (LoadBalancerHostMap map : mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lb.getId())) {
            getObjectProcessManager().scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE, map,
                    null);
        }

        return null;
    }

    @Inject
    public void setLoadBalancerTargetDao(LoadBalancerTargetDao LoadBalancerTargetDao) {
        this.lbTargetDao = LoadBalancerTargetDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
