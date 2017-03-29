package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRemove extends AbstractObjectProcessHandler {
    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        
        sdService.remove(service);

        return null;
    }
}
