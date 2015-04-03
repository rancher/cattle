package io.cattle.platform.iaas.api.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigListenerMapTable.LOAD_BALANCER_CONFIG_LISTENER_MAP;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
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
    ObjectProcessManager objectProcessManager;

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
            LoadBalancerConfigListenerMap configListenerMap = mapDao.findNonRemoved(LoadBalancerConfigListenerMap.class, LoadBalancerConfig.class, config
                    .getId(), LoadBalancerListener.class, listenerId);
            if (configListenerMap == null) {
                configListenerMap = objectManager.create(LoadBalancerConfigListenerMap.class, LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_CONFIG_ID, config
                        .getId(), LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_LISTENER_ID, listenerId);
            }
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE,
                    configListenerMap, null);
        }
    }

    private void removeOldListenerMaps(LoadBalancerConfig config, List<? extends Long> newListenerIds) {
        List<? extends LoadBalancerConfigListenerMap> existingMaps = mapDao.findToRemove(LoadBalancerConfigListenerMap.class, LoadBalancerConfig.class, config
                .getId());

        List<LoadBalancerConfigListenerMap> mapsToRemove = new ArrayList<>();

        for (LoadBalancerConfigListenerMap existingListenerId : existingMaps) {
            if (!newListenerIds.contains(existingListenerId.getLoadBalancerListenerId())) {
                mapsToRemove.add(existingListenerId);
            }
        }

        for (LoadBalancerConfigListenerMap mapToRemove : mapsToRemove) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                    mapToRemove, null);
        }
    }

}
