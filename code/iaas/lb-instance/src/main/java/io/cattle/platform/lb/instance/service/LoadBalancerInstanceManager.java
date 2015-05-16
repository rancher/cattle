package io.cattle.platform.lb.instance.service;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;

import java.util.List;

public interface LoadBalancerInstanceManager {

    List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer);

    boolean isLbInstance(Instance instance);

    LoadBalancer getLoadBalancerForInstance(Instance lbInstance);

    Instance getLoadBalancerInstance(LoadBalancer loadBalancer, LoadBalancerHostMap hostMap);

    IpAddress getLoadBalancerInstanceIp(Instance lbInstance);

    List<Agent> getLoadBalancerAgents(LoadBalancer loadBalancer);

    List<Instance> getLoadBalancerInstances(LoadBalancer loadBalancer);

    LoadBalancerHostMap getLoadBalancerHostMapForInstance(Instance lbInstance);

    Agent getLoadBalancerAgent(LoadBalancer loadBalancer, LoadBalancerHostMap hostMap);

}
