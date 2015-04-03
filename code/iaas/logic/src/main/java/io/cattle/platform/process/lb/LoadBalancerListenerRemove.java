package io.cattle.platform.process.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerListenerRemove extends AbstractObjectProcessHandler {

    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_LISTENER_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancerListener listener = (LoadBalancerListener) state.getResource();

        // get all the config references for this listener
        List<? extends LoadBalancerConfigListenerMap> maps = mapDao.findToRemove(LoadBalancerConfigListenerMap.class, LoadBalancerListener.class, listener
                .getId());

        // remove all config-listener maps
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
