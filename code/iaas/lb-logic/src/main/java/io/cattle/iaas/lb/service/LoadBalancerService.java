package io.cattle.iaas.lb.service;

import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerHostMap;

public interface LoadBalancerService {

    void addListenerToConfig(LoadBalancerConfig config, long listenerId);

    void removeListenerFromConfig(LoadBalancerConfig config, long listenerId);

    void addTargetToLoadBalancer(LoadBalancer lb, long instanceId);

    void addTargetIpToLoadBalancer(LoadBalancer lb, String ipAddress);

    void removeTargetFromLoadBalancer(LoadBalancer lb, long instanceId);

    void removeTargetIpFromLoadBalancer(LoadBalancer lb, String ipAddress);

    /**
     * adds a particular host to the load balancer
     * 
     * @param lb
     * @param hostId
     */
    void addHostToLoadBalancer(LoadBalancer lb, long hostId);

    /**
     * requests any host to be added to the load balancer - the decision which host is delegated to allocator
     * 
     * @param lb
     * @return TODO
     */
    LoadBalancerHostMap addHostToLoadBalancer(LoadBalancer lb);

    /**
     * removes a particular host from the load balancer
     * 
     * @param lb
     * @param hostId
     */
    void removeHostFromLoadBalancer(LoadBalancer lb, long hostId);
}
