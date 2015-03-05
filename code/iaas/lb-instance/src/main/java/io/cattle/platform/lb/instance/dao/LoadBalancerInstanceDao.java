package io.cattle.platform.lb.instance.dao;

import java.util.List;

public interface LoadBalancerInstanceDao {

    List<Long> getLoadBalancerHosts(long lbId);

}
