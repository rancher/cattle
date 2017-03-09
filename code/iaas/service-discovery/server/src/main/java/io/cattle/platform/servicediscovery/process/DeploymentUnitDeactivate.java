package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityLog;
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
public class DeploymentUnitDeactivate extends AbstractObjectProcessHandler {

    @Inject
    DeploymentUnitManager duMgr;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_DU_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DeploymentUnit unit = (DeploymentUnit) state.getResource();
        boolean forCleanup = unit.getCleanup();
        Object reason = state.getData().get(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON);
        Object level = state.getData().get(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL);
        if (reason != null) {
            duMgr.cleanup(unit, reason.toString(), level.toString());
        } else if (forCleanup) {
            duMgr.cleanup(unit, ServiceConstants.AUDIT_LOG_REMOVE_BAD, ActivityLog.ERROR);
        } else {
            duMgr.deactivate(unit);
        }
        objectManager.setFields(unit, ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP, false);

        return null;
    }
}