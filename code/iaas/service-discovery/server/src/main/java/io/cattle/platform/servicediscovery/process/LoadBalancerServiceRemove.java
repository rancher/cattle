package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerServiceRemove extends AbstractObjectProcessHandler {
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (!service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            return null;
        }
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        if (lb != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_REMOVE, lb, state.getData());
        }
        return null;
    }
}
