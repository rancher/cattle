package io.cattle.iaas.lb.service.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigListenerMapTable.LOAD_BALANCER_CONFIG_LISTENER_MAP;
import static io.cattle.platform.core.model.tables.LoadBalancerHostMapTable.LOAD_BALANCER_HOST_MAP;
import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import javax.inject.Inject;

public class LoadBalancerServiceImpl implements LoadBalancerService {

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

    @Override
    public void addHostToLoadBalancer(LoadBalancer lb, long hostId) {
        LoadBalancerHostMap lbHostMap = mapDao.findNonRemoved(LoadBalancerHostMap.class, LoadBalancer.class,
                lb.getId(), Host.class, hostId);
        if (lbHostMap == null) {
            lbHostMap = resourceDao.createAndSchedule(LoadBalancerHostMap.class,
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
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE, lbHostMap,
                    null);
        }
    }
    
    @Override
    public void addListenerToConfig(LoadBalancerConfig config, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findNonRemoved(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, config.getId(),
                LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap == null) {
            lbConfigListenerMap = resourceDao.createAndSchedule(LoadBalancerConfigListenerMap.class,
                    LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_CONFIG_ID,
                    config.getId(), LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_LISTENER_ID, listenerId,
                    LOAD_BALANCER_CONFIG_LISTENER_MAP.ACCOUNT_ID, config.getAccountId());
        }
    }
    
    @Override
    public void removeListenerFromConfig(LoadBalancerConfig config, long listenerId) {
        LoadBalancerConfigListenerMap lbConfigListenerMap = mapDao.findToRemove(LoadBalancerConfigListenerMap.class,
                LoadBalancerConfig.class, config.getId(),
                LoadBalancerListener.class, listenerId);

        if (lbConfigListenerMap != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_CONFIG_LISTENER_MAP_REMOVE,
                    lbConfigListenerMap, null);
        }
    }

    @Override
    public void addTargetToLoadBalancer(LoadBalancer lb, long instanceId) {
        LoadBalancerTarget target = lbTargetDao.getLbInstanceTarget(lb.getId(), instanceId);
        if (target == null) {
            target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId,
                    LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb.getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, null,
                    LOAD_BALANCER_TARGET.ACCOUNT_ID, lb.getAccountId());
        }
        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                target, null);
    }

    @Override
    public void addTargetIpToLoadBalancer(LoadBalancer lb, String ipAddress) {
        LoadBalancerTarget target = lbTargetDao.getLbIpAddressTarget(lb.getId(), ipAddress);
        if (target == null) {
            target = objectManager.create(LoadBalancerTarget.class, LOAD_BALANCER_TARGET.INSTANCE_ID, null,
                    LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lb
                            .getId(), LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress,
                    LOAD_BALANCER_TARGET.ACCOUNT_ID, lb.getAccountId());
        }
        objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_CREATE,
                target, null);
    }

    @Override
    public void removeTargetFromLoadBalancer(LoadBalancer lb, long instanceId) {
        LoadBalancerTarget target = lbTargetDao.getLbInstanceTargetToRemove(lb.getId(), instanceId);
        if (target != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE, target,
                    null);
        }
    }

    @Override
    public void removeTargetIpFromLoadBalancer(LoadBalancer lb, String ipAddress) {
        LoadBalancerTarget target = lbTargetDao.getLbIpAddressTargetToRemove(lb.getId(), ipAddress);
        if (target != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_TARGET_MAP_REMOVE, target,
                    null);
        }
    }

    @Override
    public LoadBalancerHostMap addHostToLoadBalancer(LoadBalancer lb) {
        return resourceDao.createAndSchedule(LoadBalancerHostMap.class,
                    LOAD_BALANCER_HOST_MAP.LOAD_BALANCER_ID, lb.getId(),
                    LOAD_BALANCER_HOST_MAP.ACCOUNT_ID, lb.getAccountId());
    }

}
