package io.cattle.iaas.lb.api.service;

import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;

public interface LoadBalancerApiService {
    void addListenerToConfig(LoadBalancerConfig config, long listenerId);

    void removeListenerFromConfig(LoadBalancerConfig config, long listenerId);

    /**
     * adds a particular host to the load balancer
     * 
     * @param lb
     * @param hostId
     */
    void addHostToLoadBalancer(LoadBalancer lb, long hostId);

    /**
     * removes a particular host from the load balancer
     * 
     * @param lb
     * @param hostId
     */
    void removeHostFromLoadBalancer(LoadBalancer lb, long hostId);

    void addTargetToLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput targetInput);

    void removeTargetFromLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput toRemove);
}
