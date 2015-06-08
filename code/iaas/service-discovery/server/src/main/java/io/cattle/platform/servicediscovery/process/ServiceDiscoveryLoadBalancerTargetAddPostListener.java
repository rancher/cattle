package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
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

import org.apache.commons.lang3.tuple.Pair;

/**
 * The handler is responsible for registering Service instance within the LoadBalancer of the consuming service(s)
 * Can be triggered by 3 events: instance.start, serviceConsumeMap.create and service.create
 * The reason why instance.start was picked instead of serviceExposeMap.create - at the moment instance is registered
 * within the service, it might not be started yet
 */
@Named
public class ServiceDiscoveryLoadBalancerTargetAddPostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    GenericMapDao mapDao;

    @Inject
    LoadBalancerService lbManager;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_START,
                ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_EXPOSE_MAP_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {

        for (Pair<ServiceExposeMap, Long> instanceLBServicePair : getInstanceToLBServiceMap(state, process)) {
            LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                    instanceLBServicePair.getRight(),
                    LOAD_BALANCER.REMOVED, null);
            ServiceExposeMap map = instanceLBServicePair.getLeft();
            if (lb != null) {
                // register only instances of primary service
                if (map.getDnsPrefix() == null) {
                    if (map.getInstanceId() != null) {
                        lbManager.addTargetToLoadBalancer(lb, map.getInstanceId());
                    } else if (map.getIpAddress() != null) {
                        lbManager.addTargetIpToLoadBalancer(lb, map.getIpAddress());
                    }
                }
            }
        }

        return null;
    }

    private List<Pair<ServiceExposeMap, Long>> getInstanceToLBServiceMap(ProcessState state, ProcessInstance process) {
        List<Pair<ServiceExposeMap, Long>> exposeMapToLBService = new ArrayList<>();
        if (process.getName().equalsIgnoreCase(InstanceConstants.PROCESS_START)) {
            Instance instance = (Instance) state.getResource();
            List<ServiceExposeMap> maps = objectManager.mappedChildren(
                    objectManager.loadResource(Instance.class, instance.getId()),
                    ServiceExposeMap.class);
            if (maps.isEmpty()) {
                // handle only instances that are the part of the service
                return exposeMapToLBService;
            }
            List<? extends ServiceConsumeMap> consumingServicesMaps = consumeMapDao
                    .findConsumingServices(maps.get(0).getServiceId());
            for (ServiceConsumeMap consumingServiceMap : consumingServicesMaps) {
                Service lbService = objectManager.loadResource(Service.class, consumingServiceMap.getServiceId());
                if (lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                    exposeMapToLBService.add(Pair.of(maps.get(0), lbService.getId()));
                }
            }
        } else if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE)) {
            ServiceConsumeMap consumeMap = (ServiceConsumeMap) state.getResource();
            Service lbService = objectManager.loadResource(Service.class, consumeMap.getServiceId());
            if (lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                        objectManager.loadResource(Service.class, consumeMap.getConsumedServiceId()),
                        ServiceExposeMap.class);
                for (ServiceExposeMap map : maps) {
                    exposeMapToLBService.add(Pair.of(map, lbService.getId()));
                }
            }
        } else if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE)) {
            // handle only for lb service to handle the case when lbService.create finishes after the link is created
            // for the service
            Service lbService = (Service) state.getResource();
            if (lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                // a) get all the services consumed by the lb service
                List<? extends ServiceConsumeMap> consumedServicesMaps = consumeMapDao.findConsumedServices(lbService
                        .getId());
                // b) for every service, get the instances and register them as lb targets
                for (ServiceConsumeMap consumedServiceMap : consumedServicesMaps) {
                    List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                            objectManager.loadResource(Service.class, consumedServiceMap.getConsumedServiceId()),
                            ServiceExposeMap.class);
                    for (ServiceExposeMap map : maps) {
                        exposeMapToLBService.add(Pair.of(map, lbService
                                    .getId()));
                        }
                }
            }
        } else if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_EXPOSE_MAP_CREATE)) {
            ServiceExposeMap map = (ServiceExposeMap) state.getResource();
            if (map.getIpAddress() != null) {
                // find all services consuming the current one
                List<? extends ServiceConsumeMap> consumingServicesMaps = consumeMapDao
                        .findConsumingServices(map.getServiceId());
                for (ServiceConsumeMap consumingServiceMap : consumingServicesMaps) {
                    Service lbService = objectManager.loadResource(Service.class, consumingServiceMap.getServiceId());
                    if (lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                        exposeMapToLBService.add(Pair.of(map, lbService.getId()));
                    }
                }
            }
        }
        return exposeMapToLBService;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
