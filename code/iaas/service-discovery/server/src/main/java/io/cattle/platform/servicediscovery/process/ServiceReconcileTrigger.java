package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.manager.OnDoneActions;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.DeploymentManager;
import io.cattle.platform.servicediscovery.service.lookups.ServiceLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPostListener {
    @Inject
    DeploymentManager deploymentManager;

    @Inject
    List<ServiceLookup> serviceLookups;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                HostConstants.PROCESS_ACTIVATE,
                LabelConstants.PROCESS_HOSTLABELMAP_CREATE,
                LabelConstants.PROCESS_HOSTLABELMAP_REMOVE,
                AgentConstants.PROCESS_FINISH_RECONNECT,
                ExternalEventConstants.PROCESS_EXTERNAL_EVENT_CREATE,
                ServiceConstants.PROCESS_DU_UPDATE,
                ServiceConstants.PROCESS_SERVICE_UPDATE,
                ServiceConstants.PROCESS_DU_ERROR
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Service> services = new ArrayList<>();
        if (state.getResource() instanceof Service) {
            services.add((Service) state.getResource());
        } else {
            for (ServiceLookup lookup : serviceLookups) {
                Collection<? extends Service> lookupSvs = lookup.getServices(state.getResource());
                if (lookupSvs != null) {
                    services.addAll(lookupSvs);
                }
            }
        }

        if (services.size() > 0) {
            OnDoneActions.add(() -> deploymentManager.reconcileServices(services));
        }

        return null;
    }

}
