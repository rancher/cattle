package io.cattle.platform.api.stack;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.github.ibuildthecloud.gdapi.condition.Condition.*;

public class StackPauseAllActionHandler implements ActionHandler {

    ObjectProcessManager processManager;
    ObjectManager objectManager;

    public StackPauseAllActionHandler(ObjectProcessManager processManager, ObjectManager objectManager) {
        this.processManager = processManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }

        Stack stack = (Stack) obj;
        for (Service service : objectManager.find(Service.class,
                SERVICE.STACK_ID, stack.getId(),
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.STATE_FIELD, ne(CommonStatesConstants.REMOVING))) {
            processManager.pause(service, null);
        }

        return stack;
    }

}

