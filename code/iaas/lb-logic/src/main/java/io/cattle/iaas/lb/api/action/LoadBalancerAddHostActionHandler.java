package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerAddHostActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerApiService lbService;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_ADD_HOST;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancer)) {
            return null;
        }
        LoadBalancer lb = (LoadBalancer) obj;
        long hostId = DataAccessor.fromMap(request.getRequestObject()).withKey(LoadBalancerConstants.FIELD_LB_HOST_ID)
                .as(Long.class);
        lbService.addHostToLoadBalancer(lb, hostId);

        return objectManager.reload(lb);
    }
}
