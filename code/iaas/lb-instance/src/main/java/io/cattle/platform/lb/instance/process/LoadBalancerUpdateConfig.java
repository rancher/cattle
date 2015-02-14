package io.cattle.platform.lb.instance.process;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.tables.records.LoadBalancerConfigListenerMapRecord;
import io.cattle.platform.core.model.tables.records.LoadBalancerTargetRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerUpdateConfig extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

    @Inject
    LoadBalancerDao lbDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE,
                LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {

        boolean listenerUpdate = process.getName().equals(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_CREATE) 
                || process.getName().equals(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE);

        Set<Long> lbIds = new HashSet<>();
        if (listenerUpdate) {
            LoadBalancerConfigListenerMapRecord lbConfigMap = (LoadBalancerConfigListenerMapRecord) state.getResource();
            List<? extends LoadBalancer> lbs = lbDao.listByConfigId(Long.valueOf(lbConfigMap.getLoadBalancerConfigId()));
            for (LoadBalancer lb : lbs) {
                lbIds.add(lb.getId());
            }
        } else {
            LoadBalancerTargetRecord lbTarget = (LoadBalancerTargetRecord) state.getResource();
            lbIds.add(lbTarget.getLoadBalancerId());
        }

        for (Long lbId : lbIds) {
            LoadBalancer lb = loadResource(LoadBalancer.class, lbId);
            lbInstanceManager.createLoadBalancerInstances(lb);
            // TODO Add logic for ha proxy config update here as well as updating the config version
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
