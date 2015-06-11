package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;
import java.util.List;

public class LoadBalancerServiceDeactivatePreListener extends AbstractObjectProcessLogic implements ProcessPreListener,
        Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();

        if (!service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return null;
        }

        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);

        List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                CommonStatesConstants.UPDATING_ACTIVE);

        if (lb != null && activeStates.contains(lb.getState())) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, lb, null);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
