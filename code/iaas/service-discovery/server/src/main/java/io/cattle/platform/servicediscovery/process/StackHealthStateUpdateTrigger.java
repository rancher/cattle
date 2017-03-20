package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StackHealthStateUpdateTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    public static final String STACK = "stack-reconcile";

    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    ConfigItemStatusManager itemManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { HealthcheckConstants.PROCESS_UPDATE_HEALTHY,
                HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY, InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE, InstanceConstants.PROCESS_START,
                "service.*", "stack.*" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<Service> services = new ArrayList<>();
        Set<Long> stackIds = new HashSet<>();
        
        if (state.getResource() instanceof Stack) {
            stackIds.add(((Stack) state.getResource()).getId());
        }
        else if (state.getResource() instanceof Service) {
            services.add((Service) state.getResource());
        } else if (state.getResource() instanceof Instance) {
            services.addAll(instanceDao.findServicesFor((Instance) state.getResource()));
        }

        for (Service service : services) {
            stackIds.add(service.getStackId());
        }

        for (Long stackId : stackIds) {
            ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Stack.class,
                    stackId);
            request.addItem(STACK);
            request.withDeferredTrigger(true);
            itemManager.updateConfig(request);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

