package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

import java.util.List;

public interface ServiceDiscoveryService {

    void cleanupLoadBalancerService(Service service);

    void createLoadBalancerService(Service service);

    void removeServiceMaps(Service service);

    List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName);

    boolean isActiveService(Service service);

    void addToLoadBalancerService(Service lbSvc, ServiceExposeMap serviceInstanceToAdd);

}
