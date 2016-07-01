package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;

import java.util.List;
import java.util.Map;

public interface LoadBalancerInfoDao {
    List<LoadBalancerListenerInfo> getListeners(Service lbService);

    /*
     * This method generates set of ports for the load balancer target either from:
     * 1) ports set on the target
     * 2) if ports are not set, get information from the load balancer listener
     */
    List<LoadBalancerTargetPortSpec> getLoadBalancerTargetPorts(LoadBalancerTargetInput target,
            List<? extends LoadBalancerListenerInfo> listeners);

    List<LoadBalancerTargetInput> getLoadBalancerTargets(Service lbService);

    Map<String, Object> processLBMetadata(Service lbService, LoadBalancerInfoDao lbInfoDao, Map<String, Object> meta);

    List<LoadBalancerTargetInput> getLoadBalancerTargetsV2(Service lbService);
}
