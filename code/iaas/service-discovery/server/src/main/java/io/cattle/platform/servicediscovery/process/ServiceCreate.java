package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceCreate extends AbstractObjectProcessHandler {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    NetworkDao ntwkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        sdService.setVIP(service);
        sdService.setPorts(service);
        sdService.setToken(service);
        sdService.createInitialServiceRevision(service);

        Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
        boolean system = ServiceConstants.isSystem(stack);

        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_START_ON_CREATE)) {
            return new HandlerResult(ServiceConstants.FIELD_SYSTEM, system).withShouldContinue(true)
                    .withChainProcessName(ServiceConstants.PROCESS_SERVICE_ACTIVATE);
        }

        return new HandlerResult(ServiceConstants.FIELD_SYSTEM, system);
    }
}
