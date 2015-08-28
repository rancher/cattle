package io.cattle.platform.servicediscovery.process;


import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.impl.ServiceInstanceLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;

public class SelectorServiceCreatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        registerServiceLinks(service);
        registerInstances(service);
        return null;
    }

    protected void registerServiceLinks(Service service) {
        List<Service> services = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, service.getAccountId(),
                SERVICE.REMOVED, null);

        for (Service targetService : services) {
            // skip itself
            if (targetService.getId().equals(service.getId())) {
                continue;
            }
            if (sdService.isSelectorLinkMatch(service, targetService)) {
                sdService.addServiceLink(service, new ServiceLink(targetService.getId(), null));
            }
            if (sdService.isSelectorLinkMatch(targetService, service)) {
                sdService.addServiceLink(targetService, new ServiceLink(service.getId(), null));
            }
        }
    }

    protected void registerInstances(final Service service) {
        List<Instance> instances = objectManager.find(Instance.class, INSTANCE.ACCOUNT_ID, service.getAccountId(),
                INSTANCE.REMOVED, null);

        for (final Instance instance : instances) {
            if (sdService.isSelectorContainerMatch(service, instance.getId())) {
                lockManager.lock(new ServiceInstanceLock(service, instance), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        ServiceExposeMap exposeMap = exposeMapDao.createServiceInstanceMap(service, instance, false);
                        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                                    null);
                        }
                    }
                });
            }
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
