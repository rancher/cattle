package io.cattle.platform.iaas.api.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LoadBalancerSetTargetsActionHandler implements ActionHandler {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericMapDao mapDao;

    @Inject
    LoadBalancerTargetDao lbTargetDao;
    
    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_SET_TARGETS;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancer)) {
            return null;
        }
        LoadBalancer lb = (LoadBalancer) obj;
        List<? extends Long> newInstanceIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_IDS).asList(
                jsonMapper, Long.class);
        List<? extends String> newIpAddresses = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESSES).asList(
                jsonMapper, String.class);

        // remove old targets set
        removeOldTargetMaps(lb, newInstanceIds, newIpAddresses);

        // create a new targets set
        createNewTargetMaps(lb, newInstanceIds, newIpAddresses);

        return objectManager.reload(lb);
    }

    private void createNewTargetMaps(LoadBalancer lb, List<? extends Long> newInstanceIds, List<? extends String> newIpAddresses) {

        if (newInstanceIds != null) {
            for (Long instanceId : newInstanceIds) {
                LoadBalancerTarget target = lbTargetDao.getLbInstanceTarget(lb.getId(), instanceId);
                if (target == null) {
                    target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId,
                            LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb.getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, null);
                }
                objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                        target, null);
            }
        }

        if (newIpAddresses != null) {
            for (String ipAddress : newIpAddresses) {
                LoadBalancerTarget target = lbTargetDao.getLbIpAddressTarget(lb.getId(), ipAddress);
                if (target == null) {
                    target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, null, LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb
                            .getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress);
                }
                objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                        target, null);
            }
        }
    }

    private void removeOldTargetMaps(LoadBalancer lb, List<? extends Long> newInstanceIds, List<? extends String> newIpAddresses) {
        List<? extends LoadBalancerTarget> existingTargets = mapDao.findToRemove(LoadBalancerTarget.class, LoadBalancer.class, lb.getId());

        List<LoadBalancerTarget> targetsToRemove = new ArrayList<>();

        for (LoadBalancerTarget existingTarget : existingTargets) {
            if (existingTarget.getInstanceId() != null) {
                if (newInstanceIds != null && !newInstanceIds.contains(existingTarget.getInstanceId())) {
                    targetsToRemove.add(existingTarget);
                }
            } else {
                if (newIpAddresses != null && !newIpAddresses.contains(existingTarget.getIpAddress())) {
                    targetsToRemove.add(existingTarget);
                }
            }
        }

        for (LoadBalancerTarget targetToRemove : targetsToRemove) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE,
                    targetToRemove,
                    null);
        }
    }

}
