package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LoadBalancerServiceActivatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    LoadBalancerTargetDao targetDao;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    LoadBalancerService lbService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (!service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return null;
        }
        
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                service.getId(), LOAD_BALANCER.REMOVED, null);
        
        List<Long> consumedInstancesIds = getConsumedInstancesIds(service);
        List<Long> lbTargetIds = getLoadBalancerTargetInstancesIds(lb);

        addMissingTargets(lb, consumedInstancesIds, lbTargetIds);
        removeExtraTargets(lb, consumedInstancesIds, lbTargetIds);

        return null;
    }

    protected void removeExtraTargets(LoadBalancer lb, List<Long> consumedInstancesIds, List<Long> lbTargetIds) {
        List<Long> targetInstancesToRemove = new ArrayList<>();
        targetInstancesToRemove.addAll(lbTargetIds);
        targetInstancesToRemove.removeAll(consumedInstancesIds);
        for (Long toRemove : targetInstancesToRemove) {
            lbService.removeTargetFromLoadBalancer(lb, toRemove);
        }
    }

    protected void addMissingTargets(LoadBalancer lb, List<Long> consumedInstancesIds, List<Long> lbTargetIds) {
        List<Long> targetInstanceToAdd = new ArrayList<>();
        targetInstanceToAdd.addAll(consumedInstancesIds);
        targetInstanceToAdd.removeAll(lbTargetIds);

        for (Long toAdd : targetInstanceToAdd) {
            lbService.addTargetToLoadBalancer(lb, toAdd);
        }
    }

    protected List<Long> getConsumedInstancesIds(Service service) {
        List<Long> consumedInstancesIds = new ArrayList<>();
        // a) get all the services consumed by the lb service
        List<? extends ServiceConsumeMap> consumedServicesMaps = consumeMapDao.findConsumedServices(service
                .getId());
        // b) for every service, get the instances and register them as lb targets
        for (ServiceConsumeMap consumedServiceMap : consumedServicesMaps) {
            List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                    objectManager.loadResource(Service.class, consumedServiceMap.getConsumedServiceId()),
                    ServiceExposeMap.class);
            for (ServiceExposeMap map : maps) {
                if (map.getInstanceId() != null) {
                    consumedInstancesIds.add(map.getInstanceId());
                }
            }
        }
        return consumedInstancesIds;
    }
    
    protected List<Long> getLoadBalancerTargetInstancesIds(LoadBalancer lb) {
        List<Long> targetIds = new ArrayList<>();
        List<? extends LoadBalancerTarget> lbTargets = targetDao.getLoadBalancerActiveInstanceTargets(lb.getId());
        for (LoadBalancerTarget lbTarget : lbTargets) {
            if (lbTarget.getInstanceId() != null) {
                targetIds.add(lbTarget.getInstanceId());
            }
        }

        return targetIds;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
