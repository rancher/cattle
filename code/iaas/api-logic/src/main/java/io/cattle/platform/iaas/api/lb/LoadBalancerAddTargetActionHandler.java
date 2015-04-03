package io.cattle.platform.iaas.api.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
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

public class LoadBalancerAddTargetActionHandler implements ActionHandler {

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_ADD_TARGET;
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

        createLbTarget(lb, instanceId, ipAddress);

        return objectManager.reload(lb);
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

    protected void createLbTarget(LoadBalancer lb, Long instanceId, String ipAddress) {
        LoadBalancerTarget target = getLbTarget(lb, instanceId, ipAddress);
        if (target == null) {
            target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId, LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb
                    .getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress);
        }

        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE, target, null);
    }

}
