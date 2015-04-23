package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
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
    LoadBalancerService lbService;
    
    @Inject
    ObjectManager objectManager;

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
                lbService.addTargetToLoadBalancer(lb, instanceId);
            }
        }

        if (newIpAddresses != null) {
            for (String ipAddress : newIpAddresses) {
                lbService.addTargetIpToLoadBalancer(lb, ipAddress);
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
            if (targetToRemove.getIpAddress() != null) {
                lbService.removeTargetIpFromLoadBalancer(lb, targetToRemove.getIpAddress());
            } else {
                lbService.removeTargetFromLoadBalancer(lb, targetToRemove.getInstanceId());
            }
        }
    }

}
