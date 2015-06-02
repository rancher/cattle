package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalDeploymentUnitInstance extends DeploymentUnitInstance {
    protected String ipAddress;
    List<String> serviceExternalIps;


    @SuppressWarnings("unchecked")
    protected ExternalDeploymentUnitInstance(String uuid, Service service, DeploymentServiceContext context, String ipAddress, String launchConfigName) {
        super(context, uuid, service, launchConfigName);
        this.ipAddress = ipAddress;
        this.exposeMap = context.exposeMapDao.getServiceIpExposeMap(service, ipAddress);
        this.serviceExternalIps = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                .as(List.class);
    }

    @Override
    public boolean isError() {
        return !serviceExternalIps.contains(this.ipAddress);
    }

    @Override
    protected void removeUnitInstance() {
        return;
    }

    @Override
    public void stop() {
        super.remove();
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
