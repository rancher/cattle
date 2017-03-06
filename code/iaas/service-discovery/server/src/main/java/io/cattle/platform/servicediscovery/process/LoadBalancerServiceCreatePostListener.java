package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.lock.ServiceDiscoveryServiceSetLinksLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class LoadBalancerServiceCreatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_CREATE, ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(service), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    Set<Long> newServiceIds = new HashSet<>();
                    Set<Long> oldServiceIds = new HashSet<>();
                    List<Service> targetServices = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, service.getAccountId(),
                            SERVICE.REMOVED, null);
                    setLoadBalancerServiceLinkedServicesIds(state, process, service, oldServiceIds, newServiceIds, targetServices);

                    oldServiceIds.removeAll(newServiceIds);
                    for (Long newServiceId : newServiceIds) {
                        addServiceLink(service, newServiceId);
                    }

                    for (Long oldServiceId : oldServiceIds) {
                        removeServiceLink(service, oldServiceId);
                    }
                }
            });
        } else {
            List<Service> targetServices = Arrays.asList(service);
            List<Service> lbServices = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, service.getAccountId(),
                    SERVICE.REMOVED, null, SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
            for (Service lbService : lbServices) {
                lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(lbService), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        Set<Long> newServiceIds = new HashSet<>();
                        setLoadBalancerServiceLinkedServicesIds(state, process, lbService, new HashSet<Long>(),
                                newServiceIds,
                                targetServices);
                        for (Long newServiceId : newServiceIds) {
                            addServiceLink(lbService, newServiceId);
                        }
                    }
                });
            }
        }

        return null;
    }

    protected void setLoadBalancerServiceLinkedServicesIds(ProcessState state, ProcessInstance process, Service lbService,
            Set<Long> oldServiceIds, Set<Long> newServiceIds, List<Service> targetServices) {
        LbConfig lbConfig = DataAccessor.field(lbService, ServiceConstants.FIELD_LB_CONFIG,
                jsonMapper, LbConfig.class);

        if (lbConfig != null && lbConfig.getPortRules() != null) {
            for (PortRule rule : lbConfig.getPortRules()) {
                if (!StringUtils.isEmpty(rule.getServiceId())) {
                    newServiceIds.add(Long.valueOf(rule.getServiceId()));
                } else {
                    newServiceIds.addAll(getSelectorBasedLinks(lbService, rule, targetServices));
                }
            }
        }

        if (process.getName().equalsIgnoreCase(ServiceConstants.PROCESS_SERVICE_UPDATE)) {
            Object oldObj = state.getData().get("old");
            if (oldObj != null) {
                Map<String, Object> old = CollectionUtils.toMap(oldObj);
                if (old.containsKey(ServiceConstants.FIELD_LB_CONFIG)) {
                    LbConfig config = jsonMapper
                            .convertValue(old.get(ServiceConstants.FIELD_LB_CONFIG), LbConfig.class);
                    List<PortRule> portRules = config.getPortRules();
                    if (portRules != null) {
                        for (PortRule rule : portRules) {
                            if (!StringUtils.isEmpty(rule.getServiceId())) {
                                oldServiceIds.add(Long.valueOf(rule.getServiceId()));
                            } else {
                                oldServiceIds.addAll(getSelectorBasedLinks(lbService, rule, targetServices));
                            }
                        }
                    }
                }
            }
        }
    }

    protected Set<Long> getSelectorBasedLinks(Service service, PortRule rule, List<Service> targetServices) {
        Set<Long> svcIds = new HashSet<>();
        for (Service targetService : targetServices) {
            // skip itself
            if (targetService.getId().equals(service.getId())) {
                continue;
            }
            if (sdService.isSelectorLinkMatch(rule.getSelector(), targetService)) {
                svcIds.add(targetService.getId());
            }
            if (sdService.isSelectorLinkMatch(targetService.getSelectorLink(), service)) {
                svcIds.add(targetService.getId());
            }
        }
        return svcIds;
    }


    protected void removeServiceLink(Service service, Long targetServiceId) {
        ServiceLink link = new ServiceLink(targetServiceId, null);
        sdService.removeServiceLink(service, link);
    }

    protected void addServiceLink(Service service, Long targetServiceId) {
        ServiceLink link = new ServiceLink(targetServiceId, null);
        sdService.addServiceLink(service, link);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
