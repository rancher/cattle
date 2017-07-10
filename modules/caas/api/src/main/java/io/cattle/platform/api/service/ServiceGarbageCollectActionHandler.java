package io.cattle.platform.api.service;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class ServiceGarbageCollectActionHandler implements ActionHandler {

    ServiceDao svcDao;
    ObjectProcessManager objectProcessManager;

    public ServiceGarbageCollectActionHandler(ServiceDao svcDao, ObjectProcessManager objectProcessManager) {
        super();
        this.svcDao = svcDao;
        this.objectProcessManager = objectProcessManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }

        Service service = (Service) obj;

        for (Instance instance : svcDao.getInstancesToGarbageCollect(service)) {
            objectProcessManager.stopThenRemove(instance, null);
        }

        return service;
    }
}

