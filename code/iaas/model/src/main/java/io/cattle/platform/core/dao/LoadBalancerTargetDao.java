package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;

import java.util.List;

public interface LoadBalancerTargetDao {

    LoadBalancerTarget getLbInstanceTarget(long lbId, long instanceId);

    LoadBalancerTarget getLbIpAddressTarget(long lbId, String ipAddress);

    List<? extends LoadBalancerTarget> listByLbIdToRemove(long lbId);

    List<? extends LoadBalancerTarget> listByLbId(long lbId);

    LoadBalancerTarget getLbInstanceTargetToRemove(long lbId, long instanceId);

    LoadBalancerTarget getLbIpAddressTargetToRemove(long lbId, String ipAddress);

    List<? extends Instance> getLoadBalancerActiveTargetInstances(long lbId);

    List<? extends LoadBalancerTarget> getLoadBalancerActiveIpTargets(long lbId);

    List<? extends LoadBalancerTarget> getLoadBalancerActiveInstanceTargets(long lbId);

    /*
     * This method generates set of ports for the load balancer target either from:
     * 1) ports set on the target
     * 2) if ports are not set, get information from the load balancer listener
     */
    List<LoadBalancerTargetPortSpec> getLoadBalancerTargetPorts(LoadBalancerTarget target);

    void createLoadBalancerTarget(LoadBalancer lb, List<? extends String> ports, String ipAddress, Long instanceId);

    void removeLoadBalancerTarget(LoadBalancer lb, LoadBalancerTargetInput toRemove);

}
