package io.cattle.platform.iaas.api.lb;

import static io.cattle.platform.core.model.tables.LoadBalancerHostMapTable.LOAD_BALANCER_HOST_MAP;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class LoadBalancerAddHostActionHandler implements ActionHandler {

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return LoadBalancerConstants.PROCESS_LB_ADD_HOST;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof LoadBalancer)) {
            return null;
        }
        LoadBalancer lb = (LoadBalancer) obj;
        long hostId = DataAccessor.fromMap(request.getRequestObject()).withKey(LoadBalancerConstants.FIELD_LB_HOST_ID)
                .as(Long.class);
        createLbHostMap(lb.getId(), hostId);

        return objectManager.reload(lb);
    }

    protected void createLbHostMap(long lbId, long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class, lbId, Host.class, hostId);

        if (lbHostMap == null) {
            lbHostMap = objectManager.create(LoadBalancerHostMap.class, LOAD_BALANCER_HOST_MAP.LOAD_BALANCER_ID, lbId, LOAD_BALANCER_HOST_MAP.HOST_ID, hostId);
        }
        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_HOST_MAP_CREATE, lbHostMap, null);
    }
}
