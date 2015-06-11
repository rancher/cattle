package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The handler is responsible for removing Service instance from the load balancer of the LB service consuming instance
 * service
 */

@Named
public class ServiceDiscoveryLoadBalancerTargetRemovePostListener extends AbstractObjectProcessLogic implements
        ProcessPostListener,
        Priority {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    LoadBalancerService lbManager;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_EXPOSE_MAP_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {

        if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE)) {
            ServiceConsumeMap consumeMap = (ServiceConsumeMap) state.getResource();

            // no special handling for a regular service
            Service lbService = objectManager.loadResource(Service.class, consumeMap.getServiceId());
            if (!lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                return null;
            }
            if (!sdService.isActiveService(lbService)) {
                return null;
            }
            removeConsumedServiceTargets(consumeMap, lbService);
        } else if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_EXPOSE_MAP_REMOVE)) {
            ServiceExposeMap exposeMap = (ServiceExposeMap) state.getResource();
            removeExposedMapIpFromLoadBalancer(exposeMap);
        }

        return null;
    }

    private void removeExposedMapIpFromLoadBalancer(ServiceExposeMap exposeMap) {
        if (exposeMap.getIpAddress() != null) {
            // find all services consuming the current one
            List<? extends ServiceConsumeMap> consumingServicesMaps = consumeMapDao
                    .findConsumingServices(exposeMap.getServiceId());
            for (ServiceConsumeMap consumingServiceMap : consumingServicesMaps) {
                Service lbService = objectManager.loadResource(Service.class, consumingServiceMap.getServiceId());
                if (lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                    if (!sdService.isActiveService(lbService)) {
                        return;
                    }
                    LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                            lbService.getId(),
                            LOAD_BALANCER.REMOVED, null);

                    if (lb != null) {
                        lbManager.removeTargetIpFromLoadBalancer(lb, exposeMap.getIpAddress());
                    }
                }
            }
        }
    }

    private void removeConsumedServiceTargets(ServiceConsumeMap consumeMap, Service lbService) {
        // get all the instances of the consumed service
        List<Long> instanceIds = new ArrayList<>();
        List<String> ips = new ArrayList<>();
        List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                objectManager.loadResource(Service.class, consumeMap.getConsumedServiceId()),
                ServiceExposeMap.class);
        for (ServiceExposeMap map : maps) {
            if (map.getInstanceId() != null) {
                instanceIds.add(map.getInstanceId());
            } else if (map.getIpAddress() != null) {
                ips.add(map.getIpAddress());
            }
        }

        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                lbService.getId(),
                LOAD_BALANCER.REMOVED, null);

        if (lb != null) {
            // unassign instances from the lb
            for (Long instanceId : instanceIds) {
                // to handle the case when link was created/removed in the short period of time while lb service is
                // being created
                lbManager.removeTargetFromLoadBalancer(lb, instanceId);
            }
            // unassign ips from the lb
            for (String ip : ips) {
                // to handle the case when link was created/removed in the short period of time while lb service is
                // being created
                lbManager.removeTargetIpFromLoadBalancer(lb, ip);
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
