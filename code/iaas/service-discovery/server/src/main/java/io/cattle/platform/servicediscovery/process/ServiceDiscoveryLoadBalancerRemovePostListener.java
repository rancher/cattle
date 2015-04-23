package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceDiscoveryLoadBalancerRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();
        if (lb.getServiceId() == null) {
            // config/listener cleanup gets executed for the lb created as a part of service discovery service
            return null;
        }
        LoadBalancerConfig config = objectManager.loadResource(LoadBalancerConfig.class, lb.getLoadBalancerConfigId());
        // remove listeners if they are not in use by any other lb config
        List<? extends LoadBalancerConfigListenerMap> maps = mapDao.findToRemove(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, config.getId());
        for (LoadBalancerConfigListenerMap map : maps) {
            List<? extends LoadBalancerConfigListenerMap> nonRemovedMaps = mapDao.findNonRemoved(LoadBalancerConfigListenerMap.class,
                    LoadBalancerListener.class, map.getLoadBalancerListenerId());
            if (nonRemovedMaps.size() > 1) {
                //don't remove listener used by another config
                continue;
            } else if (nonRemovedMaps.size() == 1 && nonRemovedMaps.get(0).getLoadBalancerConfigId() != config.getId()) {
                continue;
            }
            getObjectProcessManager().scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_LISTENER_REMOVE,
                    objectManager.loadResource(LoadBalancerListener.class, map.getLoadBalancerListenerId()),
                    state.getData());
        }

        // remove the config
        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_REMOVE, config,
                state.getData());
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
