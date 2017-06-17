package io.cattle.platform.loadbalancer;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

public interface LoadBalancerService {

    void removeFromLoadBalancerServices(Instance instance);

    void removeFromLoadBalancerServices(Service service);

}
