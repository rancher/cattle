package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.model.tables.records.LoadBalancerTargetRecord;

import java.util.HashSet;
import java.util.Set;

public class UpdateTargetLoadBalancerLookup implements LoadBalancerLookup {

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerTargetRecord)) {
            return lbIds;
        }

        LoadBalancerTargetRecord lbTarget = (LoadBalancerTargetRecord) obj;
        lbIds.add(lbTarget.getLoadBalancerId());

        return lbIds;
    }
}
