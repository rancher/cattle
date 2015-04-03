package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerConfigRemove extends AbstractObjectProcessHandler {
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_CONFIG_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancerConfig config = (LoadBalancerConfig) state.getResource();

        // remove all config/listener maps
        List<? extends LoadBalancerConfigListenerMap> maps = mapDao.findToRemove(LoadBalancerConfigListenerMap.class, LoadBalancerConfig.class, config.getId());

        for (LoadBalancerConfigListenerMap map : maps) {
            getObjectProcessManager().scheduleProcessInstance(
                    LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE, map, null);
        }

        return null;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }
}
