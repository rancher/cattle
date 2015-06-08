package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.process.lock.ServiceDiscoveryServiceSetLinksLock;
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
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    LockManager lockManager;


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
        final Map<Long, String> newServiceLinks = populateNewServiceLinks(request);

        lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // remove old listeners set
                removeOldServiceMaps(service, newServiceLinks);

                // create a new set
                createNewServiceMaps(service, newServiceLinks);
            }
        });
        
        return service;
    }

    @SuppressWarnings("unchecked")
    protected Map<Long, String> populateNewServiceLinks(ApiRequest request) {
        final Map<Long, String> newServiceLinks = new HashMap<>();
        final Map<String, Long> newServiceLinksMaps = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);

        if (newServiceLinksMaps != null) {
            for (String linkName : newServiceLinksMaps.keySet()) {
                newServiceLinks.put(newServiceLinksMaps.get(linkName), linkName);
            }
        }

        final List<? extends Long> newConsumedServicesIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_IDS).asList(jsonMapper,
                        Long.class);
        
        if (newConsumedServicesIds != null) {
            for (Long consumedServiceId : newConsumedServicesIds) {
                newServiceLinks.put(consumedServiceId, null);
            }
        }
        return newServiceLinks;
    }

    private void createNewServiceMaps(Service service, Map<Long, String> newServiceLinks) {
        for (Long consumedServiceId : newServiceLinks.keySet()) {
            String linkName = newServiceLinks.get(consumedServiceId);
            ServiceConsumeMap map = consumeMapDao.findNonRemovedMap(service.getId(), consumedServiceId, linkName);
            if (map == null) {
                map = objectManager.create(ServiceConsumeMap.class,
                        SERVICE_CONSUME_MAP.SERVICE_ID,
                        service.getId(), SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId,
                        SERVICE_CONSUME_MAP.ACCOUNT_ID, service.getAccountId(),
                        SERVICE_CONSUME_MAP.NAME, linkName);
            }
            objectProcessManager.scheduleProcessInstance(
                    ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                    map, null);
        }
    }

    private void removeOldServiceMaps(Service service, Map<Long, String> newServiceLinks) {
        List<? extends ServiceConsumeMap> existingMaps = consumeMapDao.findConsumedMapsToRemove(service.getId());
        List<ServiceConsumeMap> mapsToRemove = new ArrayList<>();

        for (ServiceConsumeMap existingMap : existingMaps) {
            if (!newServiceLinks.containsKey(existingMap.getConsumedServiceId())) {
                mapsToRemove.add(existingMap);
            } else {
                String newName = newServiceLinks.get(existingMap.getConsumedServiceId());
                String existingName = existingMap.getName();
                if (newName != null && existingName != null && !newName.equals(existingName)) {
                    mapsToRemove.add(existingMap);
                }
            }
        }

        for (ServiceConsumeMap mapToRemove : mapsToRemove) {
            objectProcessManager.scheduleProcessInstance(
                    ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    mapToRemove, null);
        }
    }
}
