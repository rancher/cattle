package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringListProperty;

@Named
public class ServicesReconcilePostTrigger extends AbstractObjectProcessLogic implements ProcessPostListener {
    private static final DynamicStringListProperty PROCESS_NAMES = ArchaiusUtil
            .getList("service.reconcile.posttrigger.processes");

    @Inject
    DeploymentManager deploymentManager;

    @Inject
    List<ServiceLookup> serviceLookups;

    @Override
    public String[] getProcessNames() {
        List<String> result = PROCESS_NAMES.get();
        return result.toArray(new String[result.size()]);
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
