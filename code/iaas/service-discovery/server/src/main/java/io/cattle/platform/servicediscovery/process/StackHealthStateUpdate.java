package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackHealthStateUpdate extends AbstractObjectProcessHandler implements ProcessPostListener, Priority {

    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public String[] getProcessNames() {
        return new String[] { HealthcheckConstants.PROCESS_UPDATE_HEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY, InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE, InstanceConstants.PROCESS_START,
                ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Service> services = new ArrayList<>();
        if (state.getResource() instanceof Service) {
            services.add((Service) state.getResource());
        } else if (state.getResource() instanceof Instance) {
            services.addAll(instanceDao.findServicesFor((Instance) state.getResource()));
        }
        sdService.updateHealthState(services);

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

