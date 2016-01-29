package io.cattle.platform.servicediscovery.deployment.impl.unit;

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
    String ipAddress;
    String hostName;
    List<String> serviceExternalIps;
    String serviceHostName;


    @SuppressWarnings("unchecked")
    protected ExternalDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String launchConfigName, String ipAddress, String hostName) {
        super(context, uuid, service, launchConfigName);
        if (ipAddress != null) {
            this.ipAddress = ipAddress;
            this.exposeMap = context.exposeMapDao.getServiceIpExposeMap(service, ipAddress);
            this.serviceExternalIps = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_EXTERNALIPS).withDefault(Collections.EMPTY_LIST)
                    .as(List.class);

        } else {
            this.hostName = hostName;
            this.exposeMap = context.exposeMapDao.getServiceHostnameExposeMap(service, hostName);
            this.serviceHostName = DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_HOSTNAME).as(String.class);
        }
    }

    @Override
    public boolean isError() {
        if (this.ipAddress != null) {
            return !serviceExternalIps.contains(this.ipAddress);
        } else {
            return !serviceHostName.contains(this.hostName);
        }
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
    public DeploymentUnitInstance create(Map<String, Object> deployParams) {
        if (createNew()) {
            if (this.ipAddress != null) {
                this.exposeMap = context.exposeMapDao.createIpToServiceMap(this.service, this.ipAddress);
            } else {
                this.exposeMap = context.exposeMapDao.createHostnameToServiceMap(this.service, this.hostName);
            }
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
    public DeploymentUnitInstance waitForStartImpl() {
        this.exposeMap = context.resourceMonitor.waitFor(this.exposeMap, new ResourcePredicate<ServiceExposeMap>() {
            @Override
            public boolean evaluate(ServiceExposeMap obj) {
                return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
            }
        });
        return this;
    }

    @Override
    protected boolean isStartedImpl() {
        return this.exposeMap != null && this.exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE);
    }

    @Override
    public boolean isUnhealthy() {
        return false;
    }

    @Override
    public void waitForNotTransitioning() {
        if (this.exposeMap != null) {
            this.exposeMap = context.resourceMonitor.waitForNotTransitioning(this.exposeMap);
        }
    }

    @Override
    public void waitForAllocate() {
        return;
    }

    public boolean isHealthCheckInitializing() {
        return false;
    }

    @Override
    protected DeploymentUnitInstance startImpl() {
        return this;
    }

    @Override
    public Long getServiceIndex() {
        return null;
    }
}
