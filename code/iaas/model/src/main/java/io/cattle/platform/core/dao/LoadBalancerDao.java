package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.LoadBalancer;

import java.util.List;

public interface LoadBalancerDao {

    boolean updateLoadBalancer(long lbId, Long glbId, Long weight);

    List<? extends LoadBalancer> listByConfigId(long configId);

}
