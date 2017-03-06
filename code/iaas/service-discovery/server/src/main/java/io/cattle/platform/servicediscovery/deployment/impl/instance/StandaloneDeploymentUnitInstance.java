package io.cattle.platform.servicediscovery.deployment.impl.instance;

import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;

import java.util.Map;

public class StandaloneDeploymentUnitInstance extends AbstractDeploymentUnitInstance {

    public StandaloneDeploymentUnitInstance(DeploymentUnitManagerContext context, String instanceName,
            Instance instance, String launchConfigName) {
        super(context, instanceName, instance, launchConfigName);
    }

    @Override
    public void create(Map<String, Object> deployParams) {
        instance = context.resourceDao.createAndSchedule(Instance.class, deployParams);
        setStartOnFailure();
    }

    @Override
    public void scheduleCreate() {
        if (instance != null && instance.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                    null);
        }
    }

    @Override
    public void removeImpl() {
        return;
    }

    @Override
    public boolean isRestartAlways() {
        RestartPolicy rp = DataAccessor.field(instance, DockerInstanceConstants.FIELD_RESTART_POLICY,
                context.jsonMapper,
                RestartPolicy.class);
        if (rp == null) {
            return false;
        }
        return RESTART_ALWAYS_POLICY_NAMES.contains(rp.getName());
    }
}
