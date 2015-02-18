package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.model.tables.records.LoadBalancerHostMapRecord;

import java.util.HashSet;
import java.util.Set;

public class UpdateHostLoadBalancerLookup implements LoadBalancerLookup {

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerHostMapRecord)) {
            return lbIds;
        }
        LoadBalancerHostMapRecord lbHostMap = (LoadBalancerHostMapRecord) obj;
        lbIds.add(lbHostMap.getLoadBalancerId());

        return lbIds;
    }
}
