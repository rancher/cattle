package io.cattle.platform.process.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigListenerMapTable.LOAD_BALANCER_CONFIG_LISTENER_MAP;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerConfigAddRemoveListener extends AbstractObjectProcessHandler {

    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_CONFIG_REMOVE_LISTENER,
                LoadBalancerConstants.PROCESS_LB_CONFIG_ADD_LISTENER };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancerConfig config = (LoadBalancerConfig) state.getResource();
        long listenerId = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_LISTENER_ID)
                .as(Long.class);

        if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_CONFIG_REMOVE_LISTENER)) {
            removeConfigListenerMapping(config.getId(), listenerId);
        } else if (process.getName().equals(LoadBalancerConstants.PROCESS_LB_CONFIG_ADD_LISTENER)) {
            createConfigListenerMapping(config.getId(), listenerId);
        }

        return null;
    }

    protected void removeConfigListenerMapping(long configId, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findToRemove(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, configId, LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap != null) {
            getObjectProcessManager().executeProcess(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                    lbConfigListenerMap, null);
        }
    }

    protected void createConfigListenerMapping(long configId, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findNonRemoved(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, configId, LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap == null) {
            lbConfigListenerMap = objectManager.create(LoadBalancerConfigListenerMap.class,
                    LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_CONFIG_ID, configId,
                    LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_LISTENER_ID, listenerId);
        }
        getObjectProcessManager().executeProcess(
                LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE,
                lbConfigListenerMap, null);
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
