package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceInstanceLock;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

public class SelectorInstancePostListener implements ProcessHandler {

    ServiceLifecycleManager serviceLifecycle;
    ServiceExposeMapDao exposeMapDao;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public SelectorInstancePostListener(ServiceLifecycleManager serviceLifecycle, ServiceExposeMapDao exposeMapDao, LockManager lockManager,
            ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.serviceLifecycle = serviceLifecycle;
        this.exposeMapDao = exposeMapDao;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance) state.getResource();
        List<Service> services = objectManager.find(Service.class,
                SERVICE.ACCOUNT_ID, instance.getAccountId(),
                SERVICE.REMOVED, null,
                SERVICE.SELECTOR_CONTAINER, new Condition(ConditionType.NOTNULL));

        for (final Service service : services) {
            if (serviceLifecycle.isSelectorContainerMatch(service.getSelectorContainer(), instance)) {
                lockManager.lock(new ServiceInstanceLock(service, instance), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        ServiceExposeMap exposeMap = exposeMapDao.createServiceInstanceMap(service, instance, false);
                        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                            processManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                                    null);
                        }
                    }
                });
            }
        }

        return null;
    }

}
