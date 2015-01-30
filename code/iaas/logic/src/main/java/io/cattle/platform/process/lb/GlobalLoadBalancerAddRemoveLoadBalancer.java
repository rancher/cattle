package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.GlobalLoadBalancer;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class GlobalLoadBalancerAddRemoveLoadBalancer extends AbstractObjectProcessHandler {

    LoadBalancerDao lbDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_GLB_ADD_LB,
                LoadBalancerConstants.PROCESS_GLB_REMOVE_LB };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        GlobalLoadBalancer glb = (GlobalLoadBalancer) state.getResource();
        long lbId = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_ID)
                .as(Long.class);
        Long weight = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_WEIGHT)
                .as(Long.class);

        if (process.getName().equals(LoadBalancerConstants.PROCESS_GLB_ADD_LB)) {
            updateLoadBalancer(lbId, glb.getId(), weight);
        } else if (process.getName().equals(LoadBalancerConstants.PROCESS_GLB_REMOVE_LB)) {
            updateLoadBalancer(lbId, null, null);
        }

        return null;
    }

    protected void updateLoadBalancer(long lbId, Long glbId, Long weight) {
        lbDao.updateLoadBalancer(lbId, glbId, weight);
    }

    @Inject
    public void setLoadBalancerDao(LoadBalancerDao LoadBalancerDao) {
        this.lbDao = LoadBalancerDao;
    }
}
