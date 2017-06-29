package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.object.ObjectManager;

public class ServiceIndexRemove implements ProcessHandler {

    ServiceLifecycleManager serviceLifecycleManager;
    ObjectManager objectManager;

    public ServiceIndexRemove(ServiceLifecycleManager serviceLifecycleManager, ObjectManager objectManager) {
        super();
        this.serviceLifecycleManager = serviceLifecycleManager;
        this.objectManager = objectManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ServiceIndex serviceIndex = (ServiceIndex) state.getResource();

        Service service = objectManager.loadResource(Service.class, serviceIndex.getServiceId());
        if (service == null) {
            return null;
        }

        serviceLifecycleManager.releaseIpFromServiceIndex(service, serviceIndex);
        return null;
    }
}
