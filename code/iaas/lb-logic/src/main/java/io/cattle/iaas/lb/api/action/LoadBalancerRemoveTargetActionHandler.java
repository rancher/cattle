package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerRemoveTargetActionHandler implements ActionHandler {
    @Inject
    LoadBalancerApiService lbService;

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

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
        LoadBalancerTargetInput lbTarget = DataAccessor.fromMap(request.getRequestObject()).withKey(
                LoadBalancerConstants.FIELD_LB_TARGET).as(jsonMapper, LoadBalancerTargetInput.class);

        lbService.removeTargetFromLoadBalancer(lb, lbTarget);

        return objectManager.reload(lb);
    }
}
