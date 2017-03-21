package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServicesReconcileTrigger extends AbstractObjectProcessHandler {

    @Inject
    DeploymentManager deploymentManager;

    @Inject
    List<ServiceLookup> serviceLookups;

    @Override
    public String[] getProcessNames() {
        return new String[] { HealthcheckConstants.PROCESS_UPDATE_HEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY, InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE, ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Service> services = new ArrayList<>();
        if (state.getResource() instanceof Service) {
            services.add((Service) state.getResource());
        } else {
            if (state.getResource() instanceof Instance) {
                if (state.getData().containsKey(InstanceConstants.PROCESS_DATA_ERROR)
                        || state.getData().containsKey(ServiceConstants.PROCESS_DATA_SERVICE_RECONCILE)) {
                    return null;
                }
            }
            for (ServiceLookup lookup : serviceLookups) {
                Collection<? extends Service> lookupSvs = lookup.getServices(state.getResource());
                if (lookupSvs != null) {
                    services.addAll(lookupSvs);
                }
            }
        }

        deploymentManager.reconcileServices(services);

        return null;
    }

}
