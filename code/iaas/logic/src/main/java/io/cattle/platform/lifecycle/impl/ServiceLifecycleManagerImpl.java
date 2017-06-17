package io.cattle.platform.lifecycle.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;

import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;

import javax.inject.Inject;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceExposeMapDao serviceExposeMapDao;
    @Inject
    RevisionManager revisionManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    LoadBalancerService loadbalancerService;


    @Override
    public void preRemove(Instance instance) {
        serviceExposeMapDao.deleteServiceExposeMaps(instance);
    }

    @Override
    public void postRemove(Instance instance) {
        cleanupHealthcheckMaps(instance);
        loadbalancerService.removeFromLoadBalancerServices(instance);
        revisionManager.leaveDeploymentUnit(instance);
    }

    private void cleanupHealthcheckMaps(Instance instance) {
        HealthcheckInstance hi = objectManager.findAny(HealthcheckInstance.class, HEALTHCHECK_INSTANCE.INSTANCE_ID,
                instance.getId(),
                HEALTHCHECK_INSTANCE.REMOVED, null);

        if (hi == null) {
            return;
        }

        List<? extends HealthcheckInstanceHostMap> hostMaps = objectManager.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID, hi.getId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);

        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            processManager.remove(hostMap, null);
        }

        processManager.remove(hi, null);
    }
}
