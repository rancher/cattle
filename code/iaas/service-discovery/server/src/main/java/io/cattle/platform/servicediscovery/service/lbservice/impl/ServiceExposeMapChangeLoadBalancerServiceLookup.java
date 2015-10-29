package io.cattle.platform.servicediscovery.service.lbservice.impl;

import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ServiceExposeMapChangeLoadBalancerServiceLookup implements LoadBalancerServiceLookup {
    @Inject
    ServiceDao serviceDao;
    @Inject
    InstanceDao instanceDao;

    @Override
    public List<? extends Service> getLoadBalancerServices(Object obj) {
        if (!(obj instanceof ServiceExposeMap)) {
            return null;
        }
        ServiceExposeMap map = (ServiceExposeMap) obj;
        List<Service> lbServices = new ArrayList<>();
        lbServices.addAll(serviceDao.getConsumingLbServices(map.getServiceId()));
        return lbServices;
    }
}

