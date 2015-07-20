package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
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
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;

@Named
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

    @Inject
    JsonMapper jsonMapper;

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
        
        List<? extends ServiceExposeMap> serviceTargets = getConsumedLoadBalancerTargets(service);
        List<? extends LoadBalancerTarget> lbTargets = getLoadBalancerTargets(lb);

        addMissingTargets(service, lb, serviceTargets, lbTargets);

        return null;
    }

    @SuppressWarnings("unchecked")
    protected void addMissingTargets(Service lbSvc, LoadBalancer lb, List<? extends ServiceExposeMap> serviceTargets,
            List<? extends LoadBalancerTarget> lbTargets) {
        List<Long> lbTargetInstanceIds = (List<Long>) CollectionUtils.collect(lbTargets,
                TransformerUtils.invokerTransformer("getInstanceId"));
        List<String> lbTargetIpAddresses = (List<String>) CollectionUtils.collect(lbTargets,
                TransformerUtils.invokerTransformer("getIpAddress"));
        
        List<Long> lbServiceTargetInstanceIds = (List<Long>) CollectionUtils.collect(serviceTargets,
                TransformerUtils.invokerTransformer("getInstanceId"));
        List<String> lbServiceTargetIpAddresses = (List<String>) CollectionUtils.collect(serviceTargets,
                TransformerUtils.invokerTransformer("getIpAddress"));

        addMissingTargets(lbSvc, serviceTargets, lbTargetInstanceIds, lbTargetIpAddresses);
        removeExtraTargets(lb, lbTargets, lbServiceTargetInstanceIds, lbServiceTargetIpAddresses);
    }

    protected void removeExtraTargets(LoadBalancer lb, List<? extends LoadBalancerTarget> lbTargets,
            List<Long> lbServiceTargetInstances, List<String> lbServiceTargetIpAddresses) {
        List<LoadBalancerTarget> targetsToRemove = new ArrayList<>();
        for (LoadBalancerTarget lbTarget : lbTargets) {
            if (lbTarget.getInstanceId() != null && !lbServiceTargetInstances.contains(lbTarget.getInstanceId())) {
                targetsToRemove.add(lbTarget);
            } else if (lbTarget.getIpAddress() != null && !lbServiceTargetIpAddresses.contains(lbTarget.getIpAddress())) {
                targetsToRemove.add(lbTarget);
            }
        }
        for (LoadBalancerTarget targetToRemove : targetsToRemove) {
            lbService.removeTargetFromLoadBalancer(lb, new LoadBalancerTargetInput(targetToRemove, jsonMapper));
        }
    }

    protected void addMissingTargets(Service lbSvc, List<? extends ServiceExposeMap> serviceTargets,
            List<Long> lbTargetInstanceIds, List<String> lbTargetIpAddresses) {
        List<ServiceExposeMap> serviceTargetsToAdd = new ArrayList<>();
        for (ServiceExposeMap serviceTarget : serviceTargets) {
            if (serviceTarget.getInstanceId() != null && !lbTargetInstanceIds.contains(serviceTarget.getId())) {
                serviceTargetsToAdd.add(serviceTarget);
            } else if (serviceTarget.getIpAddress() != null && !lbTargetIpAddresses.contains(serviceTarget.getIpAddress())) {
                serviceTargetsToAdd.add(serviceTarget);
            }
        }
        for (ServiceExposeMap serviceTargetToAdd : serviceTargetsToAdd) {
            sdService.addToLoadBalancerService(lbSvc, serviceTargetToAdd);
        }
    }

    protected List<? extends ServiceExposeMap> getConsumedLoadBalancerTargets(Service service) {
        List<ServiceExposeMap> consumedLoadBalancerTargets = new ArrayList<>();
        // a) get all the services consumed by the lb service
        List<? extends ServiceConsumeMap> consumedServicesMaps = consumeMapDao.findConsumedServices(service
                .getId());
        // b) for every service, get the instances and register them as lb targets
        for (ServiceConsumeMap consumedServiceMap : consumedServicesMaps) {
            List<? extends ServiceExposeMap> maps = objectManager.mappedChildren(
                    objectManager.loadResource(Service.class, consumedServiceMap.getConsumedServiceId()),
                    ServiceExposeMap.class);
            consumedLoadBalancerTargets.addAll(maps);
        }
        return consumedLoadBalancerTargets;
    }
    
    protected List<? extends LoadBalancerTarget> getLoadBalancerTargets(LoadBalancer lb) {
        return targetDao.listByLbId(lb.getId());
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
