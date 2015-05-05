package io.cattle.platform.lb.instance.dao;

import io.cattle.platform.core.model.LoadBalancerHostMap;

import java.util.List;

public interface LoadBalancerInstanceDao {

    List<? extends LoadBalancerHostMap> getLoadBalancerHostMaps(long lbId);

}
