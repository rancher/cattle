package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceCreatePostHandler extends AbstractObjectProcessLogic implements
        ProcessPostListener, Priority {

    @Inject
    ServiceDiscoveryService sdSvc;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance.getReplacementFor() != null) {
            Instance replacement = objectManager.findAny(Instance.class, INSTANCE.ID, instance.getReplacementFor());
            if (replacement != null) {
                sdSvc.removeFromLoadBalancerServices(null, replacement);
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
