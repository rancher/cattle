package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitRemove extends AbstractObjectProcessHandler {

    @Inject
    DeploymentUnitManager duMgr;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_DU_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DeploymentUnit unit = (DeploymentUnit) state.getResource();
        Map<String, Object> data = state.getData();

        duMgr.remove(unit, String.valueOf(data.get(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON)),
                String.valueOf(data.get(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL)));
        return null;
    }

}
