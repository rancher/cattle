package io.cattle.platform.api.service;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class CancelUpgradeActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public CancelUpgradeActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        final Service service = (Service) obj;

        objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_SERVICE_PAUSE, service, null);
        return objectManager.reload(service);
    }

}
