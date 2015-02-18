package io.cattle.platform.lb.instance.service;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;

import java.util.List;

public interface LoadBalancerInstanceManager {

    List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer, Long... hostIds);

    boolean isLbInstance(Instance instance);

    LoadBalancer getLoadBalancerForInstance(Instance lbInstance);

    Instance getLoadBalancerInstance(LoadBalancer loadBalancer, long hostId);

    IpAddress getLoadBalancerInstanceIp(Instance lbInstance);

    List<Agent> getLoadBalancerAgents(LoadBalancer loadBalancer);

    List<Instance> getLoadBalancerInstances(LoadBalancer loadBalancer);

}
