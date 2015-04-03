package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRemove extends AbstractObjectProcessHandler {

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    GenericMapDao mapDao;

    @Inject
    ProcessProgress progress;

    @Inject
    ServiceDiscoveryService sdServer;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        List<Instance> instances = objectManager.mappedChildren(service, Instance.class);

        if (!instances.isEmpty()) {
            progress.init(state, sdServer.getWeights(instances.size() * 2, 100));
            removeInstances(instances);
        }
        removeServiceMaps(service);
        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE };
    }

    private void removeInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                        CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION,
                                true));
            }
        }
    }

    private void removeServiceMaps(Service service) {
        for (ServiceConsumeMap map : mapDao.findToRemove(ServiceConsumeMap.class, Service.class, service.getId())) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }
}
