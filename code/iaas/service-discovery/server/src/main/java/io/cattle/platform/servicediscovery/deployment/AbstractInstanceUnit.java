package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl;

public abstract class AbstractInstanceUnit extends DeploymentUnitInstance implements InstanceUnit {

    protected Instance instance;

    protected AbstractInstanceUnit(DeploymentManagerImpl.DeploymentServiceContext context, String uuid, Service service, String launchConfigName) {
    super(context, uuid, service, launchConfigName);
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public boolean isUnhealthy() {
        if (instance != null) {
            return instance.getHealthState() != null && (instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UNHEALTHY) || instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UPDATING_UNHEALTHY));
        }
        return false;
    }

    @Override
    public void stop() {
        if (instance != null && instance.getState().equals(InstanceConstants.STATE_RUNNING)) {
            context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                    null);
        }
    }

    @Override
    public boolean isHealthCheckInitializing() {
        return instance != null && HealthcheckConstants.HEALTH_STATE_INITIALIZING.equals(instance.getHealthState());
    }
}
