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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The handler is responsible for registering Service instance within the LoadBalancer of the consuming service(s)
 * Can be triggered by 2 events: instance.start and service_consume_map creation
 * The reason why instance.start was picked instead of service_expose_map.create - at the moment instance is registered
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
                ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Pair<Long, Long>> instancesToService = getInstanceToServiceMap(state, process);

        for (Pair<Long, Long> instanceServicePair : instancesToService) {
            // get all the services consuming the current service
            List<? extends ServiceConsumeMap> consumedByServicesMaps = consumeMapDao
                    .findConsumingServices(instanceServicePair.getRight());
            for (ServiceConsumeMap consumedByServiceMap : consumedByServicesMaps) {
                Service lbService = objectManager.loadResource(Service.class, consumedByServiceMap.getServiceId());
                if (!lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                    continue;
                }
                // if the consuming service is of LB type, have to register all instances of the current service within
                // the Load Balancer of consuming service
                LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                        lbService.getId(),
                        LOAD_BALANCER.REMOVED, null);

                lbManager.addTargetToLoadBalancer(lb, instanceServicePair.getLeft());
            }
        }

        return null;
    }

    private List<Pair<Long, Long>> getInstanceToServiceMap(ProcessState state, ProcessInstance process) {
        List<Pair<Long, Long>> instancesToService = new ArrayList<>();
        if (process.getName().equalsIgnoreCase(InstanceConstants.PROCESS_START)) {
            Instance instance = (Instance) state.getResource();
            Pair<Long, Long> instanceToService = sdService.getInstanceToServicePair(instance);
            if (instanceToService == null) {
                // handle only instances that are the part of the service
                return instancesToService;
            }
            instancesToService.add(instanceToService);
        } else {
            ServiceConsumeMap consumeMap = (ServiceConsumeMap) state.getResource();
            List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                    objectManager.loadResource(Service.class, consumeMap.getConsumedServiceId()),
                    ServiceExposeMap.class);
            for (ServiceExposeMap map : maps) {
                instancesToService.add(new ImmutablePair<Long, Long>(map.getInstanceId(), maps.get(0)
                        .getServiceId()));
            }
        }
        return instancesToService;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
