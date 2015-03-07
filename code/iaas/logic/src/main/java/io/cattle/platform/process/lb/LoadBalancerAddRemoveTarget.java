package io.cattle.platform.process.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerAddRemoveTarget extends AbstractObjectProcessHandler {

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_ADD_TARGET, LoadBalancerConstants.PROCESS_LB_REMOVE_TARGET };

    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();
        Long instanceId = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID).as(Long.class);
        String ipAddress = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS).as(String.class);

        if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_ADD_TARGET)) {
            createLbTarget(lb, instanceId, ipAddress);
        } else if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_REMOVE_TARGET)) {
            removeLbTarget(lb, instanceId, ipAddress);
        }

        return null;
    }

    protected void removeLbTarget(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = getLbTargetToRemove(lb, instanceId, ipAddress);
        if (target != null) {
            getObjectProcessManager().executeProcess(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE, target, null);
        }
    }

    private LoadBalancerTarget getLbTarget(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = null;
        if (ipAddress != null) {
            target = lbTargetDao.getLbIpAddressTarget(lb.getId(), ipAddress);
        } else {
            target = lbTargetDao.getLbInstanceTarget(lb.getId(), instanceId);
        }
        return target;
    }

    private LoadBalancerTarget getLbTargetToRemove(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = null;
        if (ipAddress != null) {
            target = lbTargetDao.getLbIpAddressTargetToRemove(lb.getId(), ipAddress);
        } else {
            target = lbTargetDao.getLbInstanceTargetToRemove(lb.getId(), instanceId);
        }
        return target;
    }

    protected void createLbTarget(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = getLbTarget(lb, instanceId, ipAddress);
        if (target == null) {
            target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId, LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb
                    .getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress);
        }

        getObjectProcessManager().executeProcess(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE, target, null);
    }

}
