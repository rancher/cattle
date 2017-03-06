package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.ExternalEventConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.LabelConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.lookups.ServiceLookup;
import io.cattle.platform.servicediscovery.service.DeploymentManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServicesReconcilePostTrigger extends AbstractObjectProcessLogic implements ProcessPostListener {
    @Inject
    DeploymentManager deploymentManager;

    @Inject
    List<ServiceLookup> serviceLookups;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                HostConstants.PROCESS_REMOVE,
                HostConstants.PROCESS_ACTIVATE,
                LabelConstants.PROCESS_HOSTLABELMAP_CREATE,
                LabelConstants.PROCESS_HOSTLABELMAP_REMOVE,
                AgentConstants.PROCESS_RECONNECT,
                AgentConstants.PROCESS_FINISH_RECONNECT,
                ExternalEventConstants.PROCESS_EXTERNAL_EVENT_CREATE
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Service> services = new ArrayList<>();
        for (ServiceLookup lookup : serviceLookups) {
            Collection<? extends Service> lookupSvs = lookup.getServices(state.getResource());
            if (lookupSvs != null) {
                services.addAll(lookupSvs);
            }
        }

        deploymentManager.reconcileServices(services);

        return null;
    }

}
