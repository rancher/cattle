package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.Service;

import java.util.List;
import java.util.Map;

public interface LoadBalancerInfoDao {
    List<LoadBalancerListenerInfo> getListeners(Service lbService);

    List<LoadBalancerTargetInput> getLoadBalancerTargets(Service lbService);

    Map<String, Object> processLBMetadata(Service lbService, LoadBalancerInfoDao lbInfoDao, Map<String, Object> meta);

    List<LoadBalancerTargetInput> getLoadBalancerTargetsV2(Service lbService);
}
