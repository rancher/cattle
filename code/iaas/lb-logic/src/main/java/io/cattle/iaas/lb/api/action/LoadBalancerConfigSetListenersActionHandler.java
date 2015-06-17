package io.cattle.iaas.lb.api.action;

import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LoadBalancerConfigSetListenersActionHandler implements ActionHandler {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericMapDao mapDao;
    
    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerApiService lbService;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_CONFIG_SET_LISTENERS;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancerConfig)) {
            return null;
        }
        LoadBalancerConfig config = (LoadBalancerConfig) obj;
        List<? extends Long> newListenerIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(LoadBalancerConstants.FIELD_LB_LISTENER_IDS).asList(jsonMapper,
                Long.class);

        if (newListenerIds != null) {
            // remove old listeners set
            removeOldListenerMaps(config, newListenerIds);

            // create a new set
            createNewListenerMaps(config, newListenerIds);
        }
        return objectManager.reload(config);
    }

    private void createNewListenerMaps(LoadBalancerConfig config, List<? extends Long> newListenerIds) {
        for (Long listenerId : newListenerIds) {
            lbService.addListenerToConfig(config, listenerId);
        }
    }

    private void removeOldListenerMaps(LoadBalancerConfig config, List<? extends Long> newListenerIds) {
        List<? extends LoadBalancerConfigListenerMap> existingMaps = mapDao.findToRemove(
                LoadBalancerConfigListenerMap.class, LoadBalancerConfig.class, config
                        .getId());

        List<LoadBalancerConfigListenerMap> mapsToRemove = new ArrayList<>();

        for (LoadBalancerConfigListenerMap existingListenerId : existingMaps) {
            if (!newListenerIds.contains(existingListenerId.getLoadBalancerListenerId())) {
                mapsToRemove.add(existingListenerId);
            }
        }

        for (LoadBalancerConfigListenerMap mapToRemove : mapsToRemove) {
            lbService.removeListenerFromConfig(config, mapToRemove.getLoadBalancerListenerId());
        }
    }
}
