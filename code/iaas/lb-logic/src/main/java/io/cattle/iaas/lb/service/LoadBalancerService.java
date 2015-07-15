package io.cattle.iaas.lb.service;

import io.cattle.platform.core.addon.LoadBalancerCertificate;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerHostMap;

import java.util.Map;
import java.util.Set;

public interface LoadBalancerService {

    void addListenerToConfig(LoadBalancerConfig config, long listenerId);

    void addTargetToLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput targetInput);

    void removeTargetFromLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput targetInput);

    /**
     * requests any host to be added to the load balancer - the decision which host is delegated to allocator
     * 
     * @param lb
     * @param data TODO
     * @return TODO
     */
    LoadBalancerHostMap addHostWLaunchConfigToLoadBalancer(LoadBalancer lb, Map<String, Object> data);

    void updateLoadBalancerCertificates(LoadBalancer lb, Set<LoadBalancerCertificate> newCertsSet);
}
