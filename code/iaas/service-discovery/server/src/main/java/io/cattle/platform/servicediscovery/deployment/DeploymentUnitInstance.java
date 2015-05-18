package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.List;
import java.util.Map;

public abstract class DeploymentUnitInstance {
    protected String uuid;
    protected Service service;
    protected Map<String, String> labels;
    protected Instance instance;
    protected DeploymentServiceContext context;

    public abstract boolean isError();

    public abstract void remove();

    public abstract void stop();

    protected DeploymentUnitInstance(String uuid, Service service, DeploymentServiceContext context) {
        this.context = context;
        this.uuid = uuid;
        this.service = service;
        this.labels = context.sdService.getServiceLabels(service);
        if (this.service != null) {
            this.labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_NAME, this.service.getName());
            this.labels.put(ServiceDiscoveryConstants.LABEL_ENVIRONMENT_NAME,
                    context.objectManager.loadResource(Environment.class, this.service.getEnvironmentId()).getName());
            /*
             * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
             * we can reference a set of containers later.
             */
            this.labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, uuid);
            /*
             * Put affinity constraint on every instance to let allocator know that they should go to the same host
             */
            this.labels.put(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL
                    + ServiceDiscoveryConstants.LABEL_SERVICE_DEPLOYMENT_UNIT + AffinityOps.SOFT_EQ.getLabelSymbol()
                    + this.uuid,
                    null);
        }
    }


    public abstract DeploymentUnitInstance start(List<Integer> volumesFromInstancesIds);

    public abstract DeploymentUnitInstance waitForStart();

    public abstract boolean isStarted();

    public abstract boolean needsCleanup();

    public String getUuid() {
        return uuid;
    }

    public Service getService() {
        return service;
    }

    public Instance getInstance() {
        return instance;
    }

}
