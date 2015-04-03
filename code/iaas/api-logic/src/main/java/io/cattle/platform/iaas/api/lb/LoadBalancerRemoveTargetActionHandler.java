package io.cattle.platform.iaas.api.lb;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerRemoveTargetActionHandler implements ActionHandler {
    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_REMOVE_TARGET;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancer)) {
            return null;
        }
        LoadBalancer lb = (LoadBalancer) obj;
        Long instanceId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID).as(Long.class);
        String ipAddress = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS).as(String.class);

        removeLbTarget(lb, instanceId, ipAddress);

        return objectManager.reload(lb);
    }

    protected void removeLbTarget(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = getLbTargetToRemove(lb, instanceId, ipAddress);
        if (target != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE, target,
                    null);
        }
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
}
