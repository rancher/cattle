package io.cattle.platform.servicediscovery.service.lbservice.impl;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * handles lb service updates (ssl certificates changes)
 * and external/alias service activate/deactivate
 */
public class ServiceChangeLoadBalancerServiceLookup implements LoadBalancerServiceLookup {
    @Inject
    ServiceDao svcDao;

    @Override
    public List<? extends Service> getLoadBalancerServices(Object obj) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        List<Service> lbServices = new ArrayList<>();
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            lbServices.add(service);
        } else if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            lbServices.addAll(svcDao.getConsumingLbServices(service.getId()));
        }
        return lbServices;
    }
}
