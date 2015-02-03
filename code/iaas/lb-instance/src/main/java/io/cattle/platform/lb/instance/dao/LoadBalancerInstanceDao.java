package io.cattle.platform.lb.instance.dao;

import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Network;

import java.util.List;

public interface LoadBalancerInstanceDao {

    List<Long> getLoadBalancerHosts(long lbId);

    Network getLoadBalancerInstanceNetwork(LoadBalancer loadBalancer);
}
