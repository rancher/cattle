package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.List;

public interface DeploymentUnitInstanceFactory {

    /**
     * Creates deployment unit instance based on the service, its type and instanceName
     * 
     * @param uuid
     * @param service
     * @param instanceName
     * @param instanceObj TODO
     * @param context TODO
     * @return
     */
    public DeploymentUnitInstance createDeploymentUnitInstance(String uuid, Service service,
            String instanceName, Object instanceObj, DeploymentServiceContext context);

    /**
     * @param service
     * @param context TODO
     * @return list of deployment units per service
     */
    public List<DeploymentUnitInstance> collectServiceInstances(Service service, DeploymentServiceContext context);
}
