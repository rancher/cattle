package io.cattle.platform.servicediscovery.api.action;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.List;

public class StackActivateServicesActionHandler implements ActionHandler {

    ObjectProcessManager objectProcessManager;
    ObjectManager objectManager;

    public StackActivateServicesActionHandler(ObjectProcessManager objectProcessManager, ObjectManager objectManager) {
        super();
        this.objectProcessManager = objectProcessManager;
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack env = (Stack) obj;
        List<? extends Service> services = objectManager.find(Service.class,
                SERVICE.STACK_ID, env.getId(),
                SERVICE.REMOVED, null);

        for (Service service : services) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.ACTIVATE, service, null);
        }

        return env;
    }

}
