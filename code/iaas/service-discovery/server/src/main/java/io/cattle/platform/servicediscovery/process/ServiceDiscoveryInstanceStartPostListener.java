package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * this handler registers service discovery instance in service_expose maps
 *
 */

@Named
public class ServiceDiscoveryInstanceStartPostListener extends AbstractObjectProcessLogic implements
        ProcessPostListener,
        Priority {

    @Inject
    GenericMapDao mapDao;

    @Inject
    LoadBalancerInstanceManager lbInstanceService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_START };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        ServiceExposeMap instanceServiceMap = null;

        // 1. create service expose map if needed
        if (lbInstanceService.isLbInstance(instance)) {
            LoadBalancer lb = lbInstanceService.getLoadBalancerForInstance(instance);
            Long serviceId = lb.getServiceId();
            if (serviceId != null) {
                // for the lb instance, service map gets created only at this point
                // as the instance gets created in generic manner by AgentBuilder factory, and I avoided to make service
                // specific modifications there
                instanceServiceMap = mapDao.findNonRemoved(ServiceExposeMap.class, Instance.class,
                        instance.getId(),
                        Service.class, serviceId);

                if (instanceServiceMap == null) {
                    instanceServiceMap = exposeMapDao.createServiceInstanceMap(
                            objectManager.loadResource(Service.class, serviceId), instance);
                }
                final ServiceExposeMap serviceMapToSchedule = instanceServiceMap;
                DeferredUtils.nest(new Runnable() {
                    @Override
                    public void run() {
                        objectProcessManager
                                .scheduleStandardProcess(StandardProcess.CREATE, serviceMapToSchedule, null);
                    }
                });
            }
        } else {
            List<? extends ServiceExposeMap> instanceServiceMaps = mapDao.findNonRemoved(ServiceExposeMap.class,
                    Instance.class, instance.getId());
            if (instanceServiceMaps.isEmpty()) {
                // not a service instance
                return null;
            }
            instanceServiceMap = instanceServiceMaps.get(0);
        }

        if (instanceServiceMap != null) {
            // 2. wait for service expose map activate, and register hosts for healthchecks
            instanceServiceMap = resourceMonitor.waitFor(instanceServiceMap, new ResourcePredicate<ServiceExposeMap>() {
                @Override
                public boolean evaluate(ServiceExposeMap obj) {
                    return CommonStatesConstants.ACTIVE.equals(obj.getState());
                }
            });
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT_OVERRIDE;
    }
}
