package io.cattle.platform.iaas.api.lb;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerConfigRemoveListenerActionHandler implements ActionHandler {
    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

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

        removeConfigListenerMapping(config.getId(), listenerId);

        return objectManager.reload(config);
    }

    protected void removeConfigListenerMapping(long configId, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findToRemove(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, configId,
                LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                    lbConfigListenerMap, null);
        }
    }
}
