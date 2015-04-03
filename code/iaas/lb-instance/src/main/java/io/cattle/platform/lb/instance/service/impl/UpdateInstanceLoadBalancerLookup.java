package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.object.ObjectManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class UpdateInstanceLoadBalancerLookup implements LoadBalancerLookup {

    @Inject
    ObjectManager objectManager;

    @Inject
    LoadBalancerDao lbDao;

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof Instance)) {
            return lbIds;
        }

        Instance instance = (Instance) obj;
        List<? extends LoadBalancerTarget> targets = objectManager.mappedChildren(objectManager.loadResource(Instance.class, instance.getId()),
                LoadBalancerTarget.class);

        for (LoadBalancerTarget target : targets) {
            LoadBalancer lb = lbDao.getActiveLoadBalancerById(target.getLoadBalancerId());
            if (lb != null) {
                lbIds.add(lb.getId());
            }
        }

        return lbIds;
    }

}
