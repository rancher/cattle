package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AddServiceLinkActionHandler implements ActionHandler {
    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_SERVICE_ADD_SERVICE_LINK;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        Long consumedServiceId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_ID).as(Long.class);

        createMap(service, consumedServiceId);

        return service;
    }

    protected void createMap(Service service, long consumedServiceId) {
        ServiceConsumeMap map = consumeMapDao.findNonRemovedMap(service.getId(), consumedServiceId);

        if (map == null) {
            map = objectManager.create(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    service.getId(), SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId,
                    SERVICE_CONSUME_MAP.ACCOUNT_ID, service.getAccountId());
        }
        objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                map, null);
    }
}
