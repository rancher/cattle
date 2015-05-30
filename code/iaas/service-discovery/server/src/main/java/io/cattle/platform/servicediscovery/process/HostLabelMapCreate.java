package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostLabelMapCreate extends AbstractDefaultProcessHandler {
    @Inject
    DeploymentManager deploymentManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HostLabelMap mapping = (HostLabelMap) state.getResource();
        Long hostId = mapping.getHostId();
        Long accountId = mapping.getAccountId();

        deploymentManager.activateGlobalServicesForHost(accountId, hostId);

        return null;
    }

}
