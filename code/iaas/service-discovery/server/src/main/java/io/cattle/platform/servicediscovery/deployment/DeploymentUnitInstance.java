package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Map;

public abstract class DeploymentUnitInstance {
    protected String uuid;
    protected Service service;
    protected DeploymentServiceContext context;
    protected ServiceExposeMap exposeMap;

    public abstract boolean isError();

    public void remove() {
        removeUnitInstance();
        if (exposeMap != null) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, exposeMap, null);
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

    protected DeploymentUnitInstance(DeploymentServiceContext context, String uuid, Service service) {
        this.context = context;
        this.uuid = uuid;
        this.service = service;
    }

    public abstract DeploymentUnitInstance start(Map<String, Object> deployParams);

    public abstract boolean createNew();

    public abstract DeploymentUnitInstance waitForStart();

    public abstract boolean isStarted();

    public abstract boolean isUnhealthy();

    public String getUuid() {
        return uuid;
    }

    public Service getService() {
        return service;
    }
}
