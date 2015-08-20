package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;

import java.util.List;

public interface LoadBalancerTargetDao {

    LoadBalancerTarget getLoadBalancerTarget(long lbId, LoadBalancerTargetInput targetInput);

    List<? extends LoadBalancerTarget> listByLbIdToRemove(long lbId);

    List<? extends LoadBalancerTarget> listByLbId(long lbId);

    List<? extends Instance> getLoadBalancerActiveTargetInstances(long lbId);

    List<? extends LoadBalancerTarget> getLoadBalancerActiveIpTargets(long lbId);

    List<? extends LoadBalancerTarget> getLoadBalancerActiveInstanceTargets(long lbId);

    /*
     * This method generates set of ports for the load balancer target either from:
     * 1) ports set on the target
     * 2) if ports are not set, get information from the load balancer listener
     */
    List<LoadBalancerTargetPortSpec> getLoadBalancerTargetPorts(LoadBalancerTarget target, LoadBalancerConfig config);

    void createLoadBalancerTarget(LoadBalancer lb, LoadBalancerTargetInput toAdd);

    void removeLoadBalancerTarget(LoadBalancer lb, LoadBalancerTargetInput toRemove);

}
