package io.cattle.platform.servicediscovery.service.lbservice.impl;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles service links updates
 */

public class LinkChangeLoadBalancerServiceLookup implements LoadBalancerServiceLookup {
    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objMgr;
    @Inject
    InstanceDao instanceDao;

    @Override
    public List<? extends Service> getLoadBalancerServices(Object obj) {
        if (!(obj instanceof ServiceConsumeMap)) {
            return null;
        }
        ServiceConsumeMap map = (ServiceConsumeMap) obj;
        List<Service> lbServices = new ArrayList<>();
        Service service = objMgr.loadResource(Service.class, map.getServiceId());
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            lbServices.add(service);
        } else {
            lbServices.addAll(serviceDao.getConsumingLbServices(service.getId()));
        }
        return lbServices;
    }
}
