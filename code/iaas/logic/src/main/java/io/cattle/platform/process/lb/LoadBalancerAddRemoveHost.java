package io.cattle.platform.process.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerHostMapTable.LOAD_BALANCER_HOST_MAP;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerAddRemoveHost extends AbstractObjectProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_ADD_HOST, LoadBalancerConstants.PROCESS_LB_REMOVE_HOST };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();
        long hostId = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_HOST_ID).as(Long.class);
        if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_ADD_HOST)) {
            createLbHostMap(lb.getId(), hostId);

        } else if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_REMOVE_HOST)) {
            removeLbHostMap(lb.getId(), hostId);
        }

        return null;
    }

    protected void removeLbHostMap(long lbId, long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lbId, Host.class, hostId);

        if (lbHostMap != null) {
            getObjectProcessManager().executeProcess(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE, lbHostMap, null);
        }
    }

    protected void createLbHostMap(long lbId, long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class, lbId, Host.class, hostId);

        if (lbHostMap == null) {
            lbHostMap = objectManager.create(LoadBalancerHostMap.class, LOAD_BALANCER_HOST_MAP.LOAD_BALANCER_ID, lbId, LOAD_BALANCER_HOST_MAP.HOST_ID, hostId);
        }
        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_HOST_MAP_CREATE, lbHostMap, null);
    }
}
