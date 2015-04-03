package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.tables.records.LoadBalancerConfigListenerMapRecord;
import io.cattle.platform.object.ObjectManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class UpdateListenerLoadBalancerLookup implements LoadBalancerLookup {

    @Inject
    ObjectManager objectManager;


    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerConfigListenerMapRecord)) {
            return lbIds;
        }
        LoadBalancerConfigListenerMapRecord lbConfigMap = (LoadBalancerConfigListenerMapRecord) obj;

        List<? extends LoadBalancer> lbs = objectManager.mappedChildren(objectManager.loadResource(LoadBalancerConfig.class, lbConfigMap
                .getLoadBalancerConfigId()), LoadBalancer.class);

        for (LoadBalancer lb : lbs) {
            if (!(lb.getState().equals(CommonStatesConstants.REMOVING) || lb.getState()
                    .equals(CommonStatesConstants.REMOVED))) {
                lbIds.add(lb.getId());
            }
        }

        return lbIds;
    }

}
