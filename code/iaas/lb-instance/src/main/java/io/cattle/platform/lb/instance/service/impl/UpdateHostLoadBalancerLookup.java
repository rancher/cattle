package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.tables.records.LoadBalancerHostMapRecord;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;


public class UpdateHostLoadBalancerLookup implements LoadBalancerLookup {
    @Inject
    LoadBalancerDao lbDao;

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerHostMapRecord)) {
            return lbIds;
        }
        LoadBalancerHostMapRecord lbHostMap = (LoadBalancerHostMapRecord) obj;
        LoadBalancer lb = lbDao.getActiveLoadBalancerById(lbHostMap.getLoadBalancerId());
        if (lb != null) {
            lbIds.add(lb.getId());
        }
        return lbIds;
    }
}
