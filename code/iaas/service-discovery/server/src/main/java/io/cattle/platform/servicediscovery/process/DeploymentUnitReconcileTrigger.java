package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    DeploymentUnitManager duMgr;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE, ServiceConstants.PROCESS_DU_UPDATE_UNHEALTHY };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DeploymentUnit unit = null;
        if (state.getResource() instanceof DeploymentUnit) {
            unit = (DeploymentUnit) state.getResource();
        } else if (state.getResource() instanceof Instance) {
            Instance instance = (Instance) state.getResource();
            if (instance.getDeploymentUnitUuid() == null) {
                return null;
            }
            if (state.getData().containsKey(InstanceConstants.PROCESS_DATA_ERROR)
                    || state.getData().containsKey(ServiceConstants.PROCESS_DATA_SERVICE_RECONCILE)) {
                return null;
            }
            unit = objectManager.findAny(DeploymentUnit.class, DEPLOYMENT_UNIT.ID,
                    instance.getDeploymentUnitId(), DEPLOYMENT_UNIT.REMOVED, null, DEPLOYMENT_UNIT.STATE,
                    new Condition(
                            ConditionType.NE, CommonStatesConstants.REMOVING));
        }

        if (unit == null) {
            return null;
        }
        duMgr.scheduleReconcile(unit);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
