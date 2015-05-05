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
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {

        List<Pair<Long, Long>> instancesToService = new ArrayList<>();
        ServiceConsumeMap consumeMap = (ServiceConsumeMap) state.getResource();
        List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                objectManager.loadResource(Service.class, consumeMap.getConsumedServiceId()),
                ServiceExposeMap.class);
        for (ServiceExposeMap map : maps) {
            instancesToService.add(new ImmutablePair<Long, Long>(map.getInstanceId(), maps.get(0)
                    .getServiceId()));
        }

        for (Pair<Long, Long> instanceServicePair : instancesToService) {
            // get all the services consuming the current service
            List<? extends ServiceConsumeMap> consumedByServicesMaps = consumeMapDao
                    .findConsumingServices(instanceServicePair.getRight());
            for (ServiceConsumeMap consumedByServiceMap : consumedByServicesMaps) {
                Service lbService = objectManager.loadResource(Service.class, consumedByServiceMap.getServiceId());
                if (!lbService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                    continue;
                }
                // if the consuming service is of LB type, remove the instance from those load balancers
                LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                        lbService.getId(),
                        LOAD_BALANCER.REMOVED, null);

                if (lb != null) {
                    // to handle the case when link was created/removed in the short period of time while lb service is
                    // being created
                    lbManager.removeTargetFromLoadBalancer(lb, instanceServicePair.getLeft());
                }
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
