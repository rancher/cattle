package io.cattle.platform.servicediscovery.process;


import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SelectorServiceCreatePostListener implements ProcessHandler {

    ServiceLifecycleManager serviceLifecycleManager;
    ServiceExposeMapDao exposeMapDao;
    LockManager lockManager;
    ServiceConsumeMapDao consumeMapDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public SelectorServiceCreatePostListener(ServiceLifecycleManager serviceLifecycleManager, ServiceExposeMapDao exposeMapDao, LockManager lockManager,
            ServiceConsumeMapDao consumeMapDao, ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.serviceLifecycleManager = serviceLifecycleManager;
        this.exposeMapDao = exposeMapDao;
        this.lockManager = lockManager;
        this.consumeMapDao = consumeMapDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        registerInstances(service);

        return null;
    }

    protected void registerInstances(final Service service) {
        if (StringUtils.isBlank(service.getSelectorContainer())) {
            return;
        }
        List<Instance> instances = objectManager.find(Instance.class, INSTANCE.ACCOUNT_ID, service.getAccountId(),
                INSTANCE.REMOVED, null);

        List<? extends ServiceExposeMap> current = exposeMapDao.getUnmanagedServiceInstanceMapsToRemove(service.getId());
        final Map<Long, ServiceExposeMap> currentMappedInstances = new HashMap<>();
        for (ServiceExposeMap map : current) {
            currentMappedInstances.put(map.getInstanceId(), map);
        }

        for (final Instance instance : instances) {
            boolean matched = serviceLifecycleManager.isSelectorContainerMatch(service.getSelectorContainer(), instance);
            if (matched && !currentMappedInstances.containsKey(instance.getId())) {
                lockManager.lock(new ServiceInstanceLock(service, instance), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        ServiceExposeMap exposeMap = exposeMapDao.createServiceInstanceMap(service, instance,
                                false);
                        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                            processManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                                    null);
                        }
                    }
                });
            } else if (!matched && currentMappedInstances.containsKey(instance.getId())) {
                lockManager.lock(new ServiceInstanceLock(service, instance), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        processManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, currentMappedInstances.get(instance.getId()),
                                null);
                    }
                });
            }
        }
    }

}
