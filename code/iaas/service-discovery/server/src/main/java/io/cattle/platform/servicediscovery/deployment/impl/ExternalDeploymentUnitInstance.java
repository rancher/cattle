package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.HashMap;
import java.util.Map;

public class ExternalDeploymentUnitInstance extends DeploymentUnitInstance {
    protected String ipAddress;

    protected ExternalDeploymentUnitInstance(String uuid, Service service, DeploymentServiceContext context, String ipAddress) {
        super(context, uuid, service);
        this.ipAddress = ipAddress;
        this.exposeMap = context.exposeMapDao.getServiceIpExposeMap(service, ipAddress);
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public void remove() {
        return;
    }

    @Override
    public void stop() {
        // try to remove first
        try {
            context.objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, this.exposeMap, null);
        } catch (ProcessCancelException e) {
            context.objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, this.exposeMap,
                    ProcessUtils.chainInData(new HashMap<String, Object>(),
                            StandardProcess.DEACTIVATE.toString(), StandardProcess.REMOVE.toString()));
        }
    }

    @Override
    public DeploymentUnitInstance start(Map<String, Object> deployParams) {
        if (createNew()) {
            this.exposeMap = context.exposeMapDao.createIpToServiceMap(this.service, this.ipAddress);
        }
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                context.objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, exposeMap, null);
            }
        });
        this.exposeMap = context.objectManager.reload(this.exposeMap);
        return this;
    }

    @Override
    public boolean createNew() {
        return this.exposeMap == null;
    }

    @Override
    public DeploymentUnitInstance waitForStart() {
        this.exposeMap = context.resourceMonitor.waitFor(this.exposeMap, new ResourcePredicate<ServiceExposeMap>() {
            @Override
            public boolean evaluate(ServiceExposeMap obj) {
                return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
            }
        });
        return this;
    }

    @Override
    public boolean isStarted() {
        return this.exposeMap != null && this.exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE);
    }

    @Override
    public boolean isUnhealthy() {
        return false;
    }

}
