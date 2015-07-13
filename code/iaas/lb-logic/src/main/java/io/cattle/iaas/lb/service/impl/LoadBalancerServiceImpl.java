package io.cattle.iaas.lb.service.impl;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.deferred.util.DeferredUtils;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class LoadBalancerServiceImpl implements LoadBalancerService {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    LoadBalancerTargetDao lbTargetDao;
    
    @Inject
    LoadBalancerDao lbDao;

    @Override
    public void addListenerToConfig(final LoadBalancerConfig config, final long listenerId) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbDao.addListenerToConfig(config, listenerId);
            }
        });
    }

    @Override
    public void addTargetToLoadBalancer(final LoadBalancer lb, final LoadBalancerTargetInput targetInput) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbTargetDao.createLoadBalancerTarget(lb, targetInput);
            }
        });
    }

    @Override
    public void removeTargetFromLoadBalancer(final LoadBalancer lb, final LoadBalancerTargetInput toRemove) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbTargetDao.removeLoadBalancerTarget(lb, toRemove);

            }
        });
    }

    @Override
    public LoadBalancerHostMap addHostWLaunchConfigToLoadBalancer(LoadBalancer lb, final Map<String, Object> data) {
        data.put(LoadBalancerConstants.FIELD_LB_ID, lb.getId());
        data.put("accountId", lb.getAccountId());
        return DeferredUtils.nest(new Callable<LoadBalancerHostMap>() {
            @Override
            public LoadBalancerHostMap call() throws Exception {
                return resourceDao.createAndSchedule(LoadBalancerHostMap.class, data);
            }
        });
    }
}
