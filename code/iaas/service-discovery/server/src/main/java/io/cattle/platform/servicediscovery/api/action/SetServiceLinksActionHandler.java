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
import java.util.List;

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
        final List<? extends Long> newConsumedServicesIds = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_IDS).asList(jsonMapper,
                        Long.class);
        if (newConsumedServicesIds != null) {
            lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(service), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    // remove old listeners set
                    removeOldServiceMaps(service, newConsumedServicesIds);

                    // create a new set
                    createNewServiceMaps(service, newConsumedServicesIds);
                }
            });
        }
        return service;
    }

    private void createNewServiceMaps(Service service, List<? extends Long> newConsumedServicesIds) {
        for (Long consumedServiceId : newConsumedServicesIds) {
            ServiceConsumeMap map = consumeMapDao.findNonRemovedMap(service.getId(), consumedServiceId);
            if (map == null) {
                map = objectManager.create(ServiceConsumeMap.class,
                        SERVICE_CONSUME_MAP.SERVICE_ID,
                        service.getId(), SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId);
            }
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                    map, null);
        }
    }

    private void removeOldServiceMaps(Service service, List<? extends Long> newConsumedServicesIds) {
        List<? extends ServiceConsumeMap> existingMaps = consumeMapDao.findConsumedMapsToRemove(service.getId());
        List<ServiceConsumeMap> mapsToRemove = new ArrayList<>();

        for (ServiceConsumeMap existingMap : existingMaps) {
            if (!newConsumedServicesIds.contains(existingMap.getConsumedServiceId())) {
                mapsToRemove.add(existingMap);
            }
        }

        for (ServiceConsumeMap mapToRemove : mapsToRemove) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    mapToRemove, null);
        }
    }
}
