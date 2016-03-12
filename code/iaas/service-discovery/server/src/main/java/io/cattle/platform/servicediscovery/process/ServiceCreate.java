package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
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
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        sdService.setVIP(service);
        sdService.setPorts(service);
        sdService.setToken(service);

        if (DataAccessor.fieldBool(service, ServiceDiscoveryConstants.FIELD_START_ON_CREATE)) {
            return new HandlerResult().withShouldContinue(true).withChainProcessName(ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE);
        }

        return null;
    }
}
