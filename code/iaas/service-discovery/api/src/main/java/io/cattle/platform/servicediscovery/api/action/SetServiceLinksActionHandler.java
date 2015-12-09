package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
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
        final Map<String, ServiceLink> newServiceLinks = populateNewServiceLinks(request, forLb);
        if (newServiceLinks != null) {
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

    protected Map<String, ServiceLink> populateNewServiceLinks(ApiRequest request, boolean forLb) {
        Map<String, ServiceLink> newServiceLinks = new HashMap<>();
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
                newServiceLinks.put(serviceLink.getUuid(), serviceLink);
            }
        }

        return newServiceLinks;
    }

    private void createNewServiceMaps(Service service, Map<String, ServiceLink> newServiceLinks) {
        for (ServiceLink newServiceLink : newServiceLinks.values()) {
            if (newServiceLink instanceof LoadBalancerServiceLink) {
                sdService.addLoadBalancerServiceLink(service, (LoadBalancerServiceLink) newServiceLink);
            } else {
                sdService.addServiceLink(service, newServiceLink);
            }
        }
    }

    private void removeOldServiceMaps(Service service, Map<String, ServiceLink> newServiceLinks, boolean forLb) {
        List<? extends ServiceConsumeMap> existingMaps = consumeMapDao.findConsumedMapsToRemove(service.getId());
        List<ServiceLink> linksToRemove = new ArrayList<>();

        for (ServiceConsumeMap existingMap : existingMaps) {
            ServiceLink existingLink = new ServiceLink(existingMap.getConsumedServiceId(), existingMap.getName());
            if (!newServiceLinks.containsKey(existingLink.getUuid())) {
                linksToRemove.add(existingLink);
            }
        }

        for (ServiceLink linkToRemove : linksToRemove) {
            sdService.removeServiceLink(service, linkToRemove);
        }
    }
}
