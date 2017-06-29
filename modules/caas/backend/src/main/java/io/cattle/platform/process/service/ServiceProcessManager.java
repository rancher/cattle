package io.cattle.platform.process.service;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;

public class ServiceProcessManager {

    ServiceLifecycleManager serviceLifecycle;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ServiceDao serviceDao;

    public ServiceProcessManager(ServiceLifecycleManager serviceLifecycle, ObjectManager objectManager, ObjectProcessManager processManager,
            ServiceDao serviceDao) {
        super();
        this.serviceLifecycle = serviceLifecycle;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.serviceDao = serviceDao;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        serviceLifecycle.create(service);

        Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
        boolean system = ServiceConstants.isSystem(stack);

        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_START_ON_CREATE)) {
            return new HandlerResult(ServiceConstants.FIELD_SYSTEM, system)
                    .withChainProcessName(ServiceConstants.PROCESS_SERVICE_ACTIVATE);
        }

        return new HandlerResult(ServiceConstants.FIELD_SYSTEM, system);
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        serviceLifecycle.remove((Service) state.getResource());
        return null;
    }

    public HandlerResult finishupgrade(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();

        for (Instance instance : serviceDao.getInstancesToGarbageCollect(service)) {
            processManager.stopThenRemove(instance, null);
        }

        return null;
    }


}
