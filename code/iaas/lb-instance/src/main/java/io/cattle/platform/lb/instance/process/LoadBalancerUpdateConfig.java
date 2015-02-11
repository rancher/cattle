package io.cattle.platform.lb.instance.process;

import io.cattle.platform.core.constants.LoadBalancerConstants;
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

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerUpdateConfig extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    LoadBalancerInstanceManager lbInstanceManager;

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

        Long lbId = null;
        if (listenerUpdate) {
            LoadBalancerConfigListenerMapRecord lbConfigMap = (LoadBalancerConfigListenerMapRecord) state.getResource();
            lbId = lbConfigMap.getId();
        } else {
            LoadBalancerTargetRecord lbTarget = (LoadBalancerTargetRecord) state.getResource();
            lbId = lbTarget.getLoadBalancerId();
        }

        LoadBalancer lb = loadResource(LoadBalancer.class, lbId);
        lbInstanceManager.createLoadBalancerInstances(lb);
        // TODO Add logic for ha proxy config update here as well as updating the config version
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
