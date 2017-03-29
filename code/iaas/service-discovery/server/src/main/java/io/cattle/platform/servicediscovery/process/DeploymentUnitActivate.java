package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitActivate extends AbstractObjectProcessHandler {

    @Inject
    DeploymentUnitManager duMgr;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_DU_ACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DeploymentUnit unit = (DeploymentUnit) state.getResource();
        duMgr.activate(unit);
        return null;
    }

}
