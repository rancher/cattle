package io.cattle.platform.loadbalancer;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

public interface LoadBalancerService {

    void removeFromLoadBalancerServices(DeploymentUnit unit);

    void removeFromLoadBalancerServices(Service service);

    void registerToLoadBalanceSevices(Instance instance);

}
