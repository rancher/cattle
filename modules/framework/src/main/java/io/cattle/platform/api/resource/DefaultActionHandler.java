package io.cattle.platform.api.resource;

import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Map;

public class DefaultActionHandler implements ActionHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public DefaultActionHandler(ObjectManager objectManager, ObjectProcessManager processManager) {
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());

        try {
            processManager.scheduleProcessInstance(name, obj, data);
        } catch (ProcessNotFoundException e) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }

        request.setResponseCode(ResponseCodes.ACCEPTED);
        return objectManager.reload(obj);
    }

}
