// TODO: Might move to different package (ideally also different project too)
package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostActivatePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener {
    @Inject
    DeploymentManager deploymentManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { HostConstants.PROCESS_ACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        deploymentManager.activateGlobalServicesForHost(host.getAccountId(), host.getId());

        return null;
    }

}
