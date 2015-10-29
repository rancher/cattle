package io.cattle.platform.servicediscovery.service.lbservice;

import io.cattle.platform.core.model.Service;

import java.util.List;

public interface LoadBalancerServiceLookup {
    List<? extends Service> getLoadBalancerServices(Object obj);
}
