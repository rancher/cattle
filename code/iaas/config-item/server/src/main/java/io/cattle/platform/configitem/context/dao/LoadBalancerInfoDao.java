package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LBMetadataUtil.LBMetadata;

import java.util.List;

public interface LoadBalancerInfoDao {
    List<LoadBalancerListenerInfo> getListeners(Service lbService);

    List<LoadBalancerTargetInput> getLoadBalancerTargets(Service lbService);

    LBMetadata processLBConfig(Service lbService, LoadBalancerInfoDao lbInfoDao);

    List<LoadBalancerTargetInput> getLoadBalancerTargetsV2(Service lbService);
}
