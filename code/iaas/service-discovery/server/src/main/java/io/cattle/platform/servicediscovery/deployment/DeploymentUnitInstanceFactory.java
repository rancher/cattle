package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentUnit;

import java.util.List;
import java.util.Map;

public interface DeploymentUnitInstanceFactory {

    /**
     * Creates deployment unit instance based on the service, its type and instanceName
     * @param context TODO
     * @param uuid
     * @param service
     * @param instanceName
     * @param instanceObj TODO
     * @param labels TODO
     * @param launchConfigName TODO
     * @return
     */
    public DeploymentUnitInstance createDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Object instanceObj, Map<String, String> labels, String launchConfigName);

    /**
     * @param service TODO
     * @param context TODO
     * @return list of deployment units per service
     */
    public List<DeploymentUnit> collectDeploymentUnits(List<Service> service, DeploymentServiceContext context);
}
