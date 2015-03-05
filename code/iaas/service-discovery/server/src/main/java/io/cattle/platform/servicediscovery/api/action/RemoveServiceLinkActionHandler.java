package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RemoveServiceLinkActionHandler implements ActionHandler {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE_SERVICE_LINK;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        Service service = (Service) obj;
        Long consumedServiceId = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SERVICE_ID).as(Long.class);

        removeMap(service.getId(), consumedServiceId);

        return service;
    }

    protected void removeMap(long serviceId, long consumedServiceId) {
        ServiceConsumeMap map = consumeMapDao.findMapToRemove(serviceId, consumedServiceId);

        if (map != null) {
            objectProcessManager.executeProcess(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }
}
