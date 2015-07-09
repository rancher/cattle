package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.lock.ServiceDiscoveryServiceSetLinksLock;
import io.cattle.platform.servicediscovery.api.service.ServiceDiscoveryApiService;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

public class SetServiceLinksActionHandler implements ActionHandler {
    
    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    LockManager lockManager;

    @Inject
    ServiceDiscoveryApiService sdService;


    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_SERVICE_SET_SERVICE_LINKS;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        final Service service = (Service) obj;
        final boolean forLb = service.getKind()
                .equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name());
        final Map<Long, ServiceLink> newServiceLinks = populateNewServiceLinks(request, forLb);
        if (newServiceLinks != null && !newServiceLinks.isEmpty()) {
            lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(service), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    // remove old listeners set
                    removeOldServiceMaps(service, newServiceLinks, forLb);

                    // create a new set
                    createNewServiceMaps(service, newServiceLinks);
                }
            });
        }
        
        return service;
    }

    protected Map<Long, ServiceLink> populateNewServiceLinks(ApiRequest request, boolean forLb) {
        Map<Long, ServiceLink> newServiceLinks = new HashMap<>();
        List<ServiceLink> serviceLinks = new ArrayList<>();
        if (forLb) {
            serviceLinks.addAll(DataAccessor.fromMap(request.getRequestObject()).withKey(
                    ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_LIST)
                    .asList(jsonMapper, LoadBalancerServiceLink.class));
        } else {
            serviceLinks.addAll(DataAccessor.fromMap(request.getRequestObject()).withKey(
                    ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_LIST)
                    .asList(jsonMapper, ServiceLink.class));
        }

        if (serviceLinks != null) {
            for (ServiceLink serviceLink : serviceLinks) {
                newServiceLinks.put(serviceLink.getServiceId(), serviceLink);
            }
        }

        return newServiceLinks;
    }

    private void createNewServiceMaps(Service service, Map<Long, ServiceLink> newServiceLinks) {
        for (ServiceLink newServiceLink : newServiceLinks.values()) {
            if (newServiceLink instanceof LoadBalancerServiceLink) {
                sdService.addLoadBalancerServiceLink(service, (LoadBalancerServiceLink) newServiceLink);
            } else {
                sdService.addServiceLink(service, newServiceLink);
            }
        }
    }

    private void removeOldServiceMaps(Service service, Map<Long, ServiceLink> newServiceLinks, boolean forLb) {
        List<? extends ServiceConsumeMap> existingMaps = consumeMapDao.findConsumedMapsToRemove(service.getId());
        List<ServiceLink> linksToRemove = new ArrayList<>();

        for (ServiceConsumeMap existingMap : existingMaps) {
            ServiceLink existingLink = new ServiceLink(existingMap.getConsumedServiceId(), existingMap.getName());
            if (!newServiceLinks.containsKey(existingMap.getConsumedServiceId())) {
                linksToRemove.add(existingLink);
            } else {
                ServiceLink newServiceLink = newServiceLinks.get(existingMap.getConsumedServiceId());
                
                boolean namesAreEqual = StringUtils.equalsIgnoreCase(newServiceLink.getName(), existingMap.getName());
                boolean portsAreEqual = true;
                if (forLb) {
                    LoadBalancerServiceLink newLbServiceLink = (LoadBalancerServiceLink) newServiceLink;
                    List<? extends String> newPorts = newLbServiceLink.getPorts() != null ? newLbServiceLink.getPorts()
                            : new ArrayList<String>();
                    List<? extends String> existingPorts = DataAccessor.fields(existingMap).
                            withKey(LoadBalancerConstants.FIELD_LB_TARGET_PORTS).withDefault(Collections.EMPTY_LIST)
                            .asList(jsonMapper, String.class);
                    portsAreEqual = newPorts.containsAll(existingPorts) && existingPorts.containsAll(newPorts);
                }

                if (!namesAreEqual || !portsAreEqual) {
                    linksToRemove.add(existingLink);
                }
            }
        }

        for (ServiceLink linkToRemove : linksToRemove) {
            sdService.removeServiceLink(service, linkToRemove);
        }
    }
}
