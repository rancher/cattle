package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerConfigRemoveListenerActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerService lbService;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_CONFIG_REMOVE_LISTENER;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancerConfig)) {
            return null;
        }
        LoadBalancerConfig config = (LoadBalancerConfig) obj;
        long listenerId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_LISTENER_ID).as(Long.class);

        lbService.removeListenerFromConfig(config, listenerId);

        return objectManager.reload(config);
    }
}
