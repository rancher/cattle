
package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.List;
import java.util.Map;


public abstract class DeploymentUnitInstance {
    protected String uuid;
    protected DeploymentServiceContext context;
    protected ServiceExposeMap exposeMap;
    protected String launchConfigName;
    protected Service service;
    protected Stack stack;

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

                        @Override
                        public String getMessage() {
                            return "removed state";
                        }
                    });
        }
    }

    protected abstract void removeUnitInstance();

    public abstract boolean isTransitioning();

    public abstract void stop();

    protected DeploymentUnitInstance(DeploymentServiceContext context, String uuid, Service service,
            String launchConfigName) {
        this.context = context;
        this.uuid = uuid;
        this.launchConfigName = launchConfigName;
        this.service = service;
        this.stack = context.objectManager.loadResource(Stack.class, service.getStackId());
    }

    public abstract DeploymentUnitInstance create(Map<String, Object> deployParams);

    public abstract void scheduleCreate();

    public DeploymentUnitInstance start() {
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

    public abstract void waitForAllocate();

    public abstract boolean isHealthCheckInitializing();

    public abstract ServiceIndex getServiceIndex();

    public Stack getStack() {
        return stack;
    }

    public void generateAuditLog(AuditEventType eventType, String description, String level) {
        if (this instanceof InstanceUnit) {
            InstanceUnit defaultInstance = (InstanceUnit) this;
            context.activityService.instance(defaultInstance.getInstance(), eventType.toString(), description, level);
        }
    }

    public abstract List<String> getSearchDomains();

    public abstract Long getCreateIndex();
}
