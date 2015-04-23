package io.cattle.iaas.lb.service;

import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;

public interface LoadBalancerService {

    void addHostToLoadBalancer(LoadBalancer lb, long hostId);

    void removeHostFromLoadBalancer(LoadBalancer lb, long hostId);

    void addListenerToConfig(LoadBalancerConfig config, long listenerId);

    void removeListenerFromConfig(LoadBalancerConfig config, long listenerId);

    void addTargetToLoadBalancer(LoadBalancer lb, long instanceId);

    void addTargetIpToLoadBalancer(LoadBalancer lb, String ipAddress);

    void removeTargetFromLoadBalancer(LoadBalancer lb, long instanceId);

    void removeTargetIpFromLoadBalancer(LoadBalancer lb, String ipAddress);

}
