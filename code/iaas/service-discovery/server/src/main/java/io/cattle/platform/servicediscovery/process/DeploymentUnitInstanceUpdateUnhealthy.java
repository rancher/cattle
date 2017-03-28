package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.InstanceHealthCheck.Strategy;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.util.type.Priority;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitInstanceUpdateUnhealthy extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    DeploymentUnitManager duMgr;
    @Inject
    ServiceDao svcDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { HealthcheckConstants.PROCESS_UPDATE_UNHEALTHY };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());

        InstanceHealthCheck healthCheck = DataAccessor.field(instance,
                InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);
        if (healthCheck.getStrategy() == Strategy.none) {
            return null;
        }

        if (unit.getServiceId() == null) {
            svcDao.setForCleanup(unit, true);
            duMgr.scheduleReconcile(unit);
        } else {
            if (healthCheck.getStrategy() == Strategy.recreate) {
                objectManager.setFields(objectManager.reload(unit), ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP,
                        true);
                duMgr.scheduleReconcile(unit);
            } else {
                // quorum
                if (!Arrays.asList(InstanceConstants.STATE_ERRORING, InstanceConstants.STATE_ERROR).contains(
                        unit.getState()))
                    objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_DU_ERROR,
                            unit, null);
                }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT_OVERRIDE;
    }

}
