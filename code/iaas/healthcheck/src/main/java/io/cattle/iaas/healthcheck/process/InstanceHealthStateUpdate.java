package io.cattle.iaas.healthcheck.process;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;

import javax.inject.Inject;

public class InstanceHealthStateUpdate extends AbstractObjectProcessHandler {

    @Inject
    DeploymentManager deploymentManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {"instance.updatehealthy", "instance.updateunhealthy"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();

        deploymentManager.reconcileServicesFor(instance);

        return null;
    }

}
