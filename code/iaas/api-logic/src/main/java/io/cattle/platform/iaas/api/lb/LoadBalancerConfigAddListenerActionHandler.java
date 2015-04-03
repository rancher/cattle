package io.cattle.platform.iaas.api.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigListenerMapTable.LOAD_BALANCER_CONFIG_LISTENER_MAP;
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

public class LoadBalancerConfigAddListenerActionHandler implements ActionHandler {

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

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

        createConfigListenerMapping(config.getId(), listenerId);

        return objectManager.reload(config);
    }

    protected void createConfigListenerMapping(long configId, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findNonRemoved(LoadBalancerConfigListenerMap.class, LoadBalancerConfig.class, configId,
                LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap == null) {
            lbConfigListenerMap = objectManager.create(LoadBalancerConfigListenerMap.class, LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_CONFIG_ID,
                    configId, LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_LISTENER_ID, listenerId);
        }
        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE,
                lbConfigListenerMap, null);
    }
}
