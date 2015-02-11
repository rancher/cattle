package io.cattle.platform.lb.instance.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;

import java.util.List;

public interface LoadBalancerInstanceManager {

    List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer, Long... hostIds);

    boolean isLbInstance(Instance instance);

}
