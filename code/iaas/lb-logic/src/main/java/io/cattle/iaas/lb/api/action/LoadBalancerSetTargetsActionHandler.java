package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

public class LoadBalancerSetTargetsActionHandler implements ActionHandler {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericMapDao mapDao;

    @Inject
    LoadBalancerApiService lbService;
    
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
        List<? extends LoadBalancerTargetInput> newLBTargets = DataAccessor
                .fromMap(request.getRequestObject())
                .withKey(
                        LoadBalancerConstants.FIELD_LB_TARGETS).asList(jsonMapper, LoadBalancerTargetInput.class);


        // remove old targets set
        removeOldTargetMaps(lb, newLBTargets);

        // create a new targets set
        createNewTargetMaps(lb, newLBTargets);

        return objectManager.reload(lb);
    }

    private void createNewTargetMaps(LoadBalancer lb,
            List<? extends LoadBalancerTargetInput> newLBTargets) {

        if (newLBTargets != null) {
            for (LoadBalancerTargetInput newLBTarget : newLBTargets) {
                lbService.addTargetToLoadBalancer(lb, newLBTarget);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeOldTargetMaps(LoadBalancer lb,
            List<? extends LoadBalancerTargetInput> newLBTargets) {
        List<? extends LoadBalancerTarget> existingTargets = mapDao.findToRemove(LoadBalancerTarget.class, LoadBalancer.class, lb.getId());
        List<Long> newLBTargetInstances = (List<Long>) CollectionUtils.collect(newLBTargets,
                TransformerUtils.invokerTransformer("getInstanceId"));
        List<String> newLBTargetIps = (List<String>) CollectionUtils.collect(newLBTargets,
                TransformerUtils.invokerTransformer("getIpAddress"));
        List<LoadBalancerTarget> targetsToRemove = new ArrayList<>();

        for (LoadBalancerTarget existingTarget : existingTargets) {
            if (existingTarget.getInstanceId() != null) {
                if (newLBTargetInstances != null && !newLBTargetInstances.contains(existingTarget.getInstanceId())) {
                    targetsToRemove.add(existingTarget);
                }
            } else {
                if (newLBTargetIps != null && !newLBTargetIps.contains(existingTarget.getIpAddress())) {
                    targetsToRemove.add(existingTarget);
                }
            }
        }

        for (LoadBalancerTarget targetToRemove : targetsToRemove) {
            lbService.removeTargetFromLoadBalancer(lb, new LoadBalancerTargetInput(targetToRemove.getInstanceId(),
                    targetToRemove.getIpAddress(), null));

        }
    }

}
