package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackRemove extends AbstractObjectProcessHandler {

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ServiceDiscoveryService sdServer;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack env = (Stack) state.getResource();
        List<? extends Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, env.getId(),
                SERVICE.REMOVED, null);
        if (!services.isEmpty()) {
            removeServices(services);
        }
        return null;
    }

    private void removeServices(List<? extends Service> services) {
        for (Service service : services) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE,
                        service, null);
        }
    }
}
