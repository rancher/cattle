package io.cattle.iaas.lb.api.service.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerHostMapTable.LOAD_BALANCER_HOST_MAP;
import io.cattle.iaas.lb.api.service.LoadBalancerApiService;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import javax.inject.Inject;

public class LoadBalancerApiServiceImpl implements LoadBalancerApiService {
    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Inject
    LoadBalancerDao lbDao;

    @Override
    public void addHostToLoadBalancer(final LoadBalancer lb, final long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class,
                lb.getId(), Host.class, hostId);
        if (lbHostMap == null) {
            resourceDao.createAndSchedule(LoadBalancerHostMap.class,
                    LOAD_BALANCER_HOST_MAP.LOAD_BALANCER_ID, lb.getId(),
                    LOAD_BALANCER_HOST_MAP.HOST_ID, hostId,
                    LOAD_BALANCER_HOST_MAP.ACCOUNT_ID, lb.getAccountId());

        }
    }

    @Override
    public void removeHostFromLoadBalancer(LoadBalancer lb, long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lb.getId(),
                Host.class, hostId);

        if (lbHostMap != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE,
                    lbHostMap, null);
        }
    }

    @Override
    public void removeListenerFromConfig(LoadBalancerConfig config, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findToRemove(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, config.getId(),
                LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap != null) {
            objectProcessManager.scheduleProcessInstance(
                    LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                    lbConfigListenerMap, null);
        }
    }

    @Override
    public void addTargetToLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput targetInput) {
        lbTargetDao.createLoadBalancerTarget(lb, targetInput.getPorts(), targetInput.getIpAddress(),
                targetInput.getInstanceId());
    }

    @Override
    public void removeTargetFromLoadBalancer(LoadBalancer lb, LoadBalancerTargetInput toRemove) {
                lbTargetDao.removeLoadBalancerTarget(lb, toRemove);
    }

    @Override
    public void addListenerToConfig(final LoadBalancerConfig config, final long listenerId) {
        lbDao.addListenerToConfig(config, listenerId);
    }
}
