package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class UpdateConfigLoadBalancerLookup implements LoadBalancerLookup {

    @Inject
    LoadBalancerDao lbDao;

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerConfig)) {
            return lbIds;
        }
        LoadBalancerConfig config = (LoadBalancerConfig) obj;
        List<? extends LoadBalancer> lbs = lbDao.listByConfigId(config.getId());
        for (LoadBalancer lb : lbs) {
            if (!(lb.getState().equals(CommonStatesConstants.REMOVING) || lb.getState()
                    .equals(CommonStatesConstants.REMOVED))) {
                lbIds.add(lb.getId());
            }
        }
        return lbIds;
    }

}
