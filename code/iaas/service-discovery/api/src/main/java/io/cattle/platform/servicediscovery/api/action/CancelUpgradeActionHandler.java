package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CancelUpgradeActionHandler implements ActionHandler {

    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return ServiceConstants.PROCESS_SERVICE_CANCEL_UPGRADE;
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
