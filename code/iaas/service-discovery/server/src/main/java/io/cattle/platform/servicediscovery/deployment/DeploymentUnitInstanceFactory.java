package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;

public interface DeploymentUnitInstanceFactory {

    /**
     * Creates deployment unit instance based on the service, its type and instanceName
     * @param context
     * @param uuid
     * @param service
     * @param instanceName
     * @param instanceObj
     * @param launchConfigName
     * @return
     */
    public DeploymentUnitInstance createDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Object instanceObj, String launchConfigName);

    /**
     * @param service
     * @param context
     * @return list of deployment units per service
     */
    public List<DeploymentUnit> collectDeploymentUnits(Service service, DeploymentServiceContext context);
}
