package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerConfigAddListenerActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerApiService lbService;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_CONFIG_ADD_LISTENER;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancerConfig)) {
            return null;
        }
        LoadBalancerConfig config = (LoadBalancerConfig) obj;
        long listenerId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_LISTENER_ID).as(Long.class);

        lbService.addListenerToConfig(config, listenerId);

        return objectManager.reload(config);
    }
}
