package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Map;

/**
 * TODO: Since the majority of the system references DeploymentUnitInstance and not InstanceUnit
 * and majority of the functions in DeploymentUnitInstance are abstract, we should really just combine
 * DeploymentUnitInstance and InstanceUnit into one interface and possibly have an
 * AbstractDeploymentUnitInstance class that provides some of the implementation.
 */
public abstract class DeploymentUnitInstance {
    protected String uuid;
    protected DeploymentServiceContext context;
    protected ServiceExposeMap exposeMap;
    protected String launchConfigName;
    protected Service service;

    public abstract boolean isError();

    public void remove() {
        removeUnitInstance();
        if (exposeMap != null) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, exposeMap, null);
        }
    }

    public void waitForRemoval() {
        if (exposeMap != null) {
            context.resourceMonitor.waitFor(exposeMap,
                    new ResourcePredicate<ServiceExposeMap>() {
                        @Override
                        public boolean evaluate(ServiceExposeMap obj) {
                            return CommonStatesConstants.REMOVED.equals(obj.getState());
                        }
                    });
        }
    }

    protected abstract void removeUnitInstance();

    public abstract void stop();

    protected DeploymentUnitInstance(DeploymentServiceContext context, String uuid, Service service,
            String launchConfigName) {
        this.context = context;
        this.uuid = uuid;
        this.launchConfigName = launchConfigName;
        this.service = service;
    }

    public DeploymentUnitInstance createAndStart(Map<String, Object> deployParams) {
        this.create(deployParams);
        this.start();
        return this;
    }

    protected abstract DeploymentUnitInstance create(Map<String, Object> deployParams);

    protected DeploymentUnitInstance start() {
        if (this.isStarted()) {
            return this;
        }
        return this.startImpl();
    }

    protected abstract DeploymentUnitInstance startImpl();

    public abstract boolean createNew();

    public DeploymentUnitInstance waitForStart() {
        if (this.isStarted()) {
            return this;
        }
        return this.waitForStartImpl();
    }

    protected abstract DeploymentUnitInstance waitForStartImpl();

    public boolean isStarted() {
        return isStartedImpl();
    }

    protected abstract boolean isStartedImpl();

    public abstract boolean isUnhealthy();

    public String getUuid() {
        return uuid;
    }

    public String getLaunchConfigName() {
        return launchConfigName;
    }

    public Service getService() {
        return service;
    }

    public abstract void waitForNotTransitioning();

    public abstract void waitForAllocate();

    public abstract boolean isHealthCheckInitializing();
}
