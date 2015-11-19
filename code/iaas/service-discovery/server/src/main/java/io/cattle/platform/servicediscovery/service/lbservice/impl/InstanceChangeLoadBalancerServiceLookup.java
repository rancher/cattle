package io.cattle.platform.servicediscovery.service.lbservice.impl;

import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles regular and lb instances operationss
 */

public class InstanceChangeLoadBalancerServiceLookup implements LoadBalancerServiceLookup {
    @Inject
    ServiceDao serviceDao;
    @Inject
    InstanceDao instanceDao;

    @Override
    public List<? extends Service> getLoadBalancerServices(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance instance = (Instance) obj;
        List<Service> lbServices = new ArrayList<>();
        List<? extends Service> services = instanceDao.findServicesFor(instance);
        for (Service service : services) {
            lbServices.addAll(serviceDao.getConsumingLbServices(service.getId()));
        }
        return lbServices;
    }
}
