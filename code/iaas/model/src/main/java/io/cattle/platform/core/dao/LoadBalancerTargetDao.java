package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancerTarget;

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

}
