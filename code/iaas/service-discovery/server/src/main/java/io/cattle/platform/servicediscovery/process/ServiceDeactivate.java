package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceDeactivate extends AbstractObjectProcessHandler {

    @Inject
    ObjectManager objectManager;
    
    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ProcessProgress progress;

    @Inject
    ServiceDiscoveryService sdServer;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        List<Instance> instances = objectManager.mappedChildren(service, Instance.class);
        if (!instances.isEmpty()) {
            progress.init(state, sdServer.getWeights(instances.size(), 100));
            stopInstances(instances);
        }
        return null;
    }

    private void stopInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance, null);
        }
    }
}
