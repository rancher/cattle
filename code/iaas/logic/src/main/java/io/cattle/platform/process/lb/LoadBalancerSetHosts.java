package io.cattle.platform.process.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerHostMapTable.LOAD_BALANCER_HOST_MAP;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerSetHosts extends AbstractObjectProcessHandler {
    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { LoadBalancerConstants.PROCESS_LB_SET_HOSTS };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        LoadBalancer lb = (LoadBalancer) state.getResource();
        List<? extends Long> newHostIds = DataAccessor.fromMap(state.getData()).withKey(LoadBalancerConstants.FIELD_LB_HOST_IDS).asList(jsonMapper, Long.class);

        if (newHostIds != null) {
            // remove old host set
            removeOldHostMaps(lb, newHostIds);

            // create a new set
            createNewHostMaps(lb, newHostIds);
        }
        return null;
    }

    private void createNewHostMaps(LoadBalancer lb, List<? extends Long> newHostIds) {
        for (Long hostId : newHostIds) {
            LoadBalancerHostMap lbHostMap = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class, lb.getId(), Host.class, hostId);
            if (lbHostMap == null) {
                lbHostMap = objectManager.create(LoadBalancerHostMap.class, LOAD_BALANCER_HOST_MAP.LOAD_BALANCER_ID, lb.getId(),
                        LOAD_BALANCER_HOST_MAP.HOST_ID, hostId);
            }
            objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_HOST_MAP_CREATE, lbHostMap, null);
        }
    }

    private void removeOldHostMaps(LoadBalancer lb, List<? extends Long> newHostIds) {
        List<? extends LoadBalancerHostMap> existingMaps = mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lb.getId());

        List<LoadBalancerHostMap> mapsToRemove = new ArrayList<>();

        for (LoadBalancerHostMap existingMap : existingMaps) {
            if (!newHostIds.contains(existingMap.getHostId())) {
                mapsToRemove.add(existingMap);
            }
        }

        for (LoadBalancerHostMap mapToRemove : mapsToRemove) {
            objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE, mapToRemove, null);
        }
    }
}
