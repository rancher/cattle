package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.AbstractInstanceUnit;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Map;

public class LoadBalancerDeploymentUnitInstance extends AbstractInstanceUnit {
    LoadBalancerHostMap hostMap;

    public LoadBalancerDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, LoadBalancerHostMap hostMap, Map<String, String> labels, String launchConfigName) {
        super(context, uuid, service, launchConfigName);
        this.hostMap = hostMap;
        if (hostMap != null) {
            Instance instance = context.lbInstanceMgr.getLoadBalancerInstance(this.hostMap);
            if (instance != null) {
                this.instance = instance;
                this.exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
            }
        }
    }

    @Override
    public boolean isError() {
        if (this.hostMap.getState().equals(CommonStatesConstants.ACTIVE)) {
            if (this.instance == null || this.instance.getRemoved() != null
                    || this.instance.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void removeUnitInstance() {
        context.objectProcessManager.scheduleProcessInstanceAsync(
                LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE,
                context.objectManager.reload(hostMap), null);
    }

    @Override
    public DeploymentUnitInstance start(Map<String, Object> deployParams) {
        if (createNew()) {
            LoadBalancer lb = context.objectManager.findAny(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                    service.getId(),
                    LOAD_BALANCER.REMOVED, null);

            Map<String, Object> launchConfig = ServiceDiscoveryUtil.buildServiceInstanceLaunchData(service,
                    deployParams, launchConfigName, context.allocatorService);
            this.hostMap = context.lbService.addHostWLaunchConfigToLoadBalancer(lb, launchConfig);
            this.instance = context.lbInstanceMgr.getLoadBalancerInstance(this.hostMap);
            this.exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
        } else if (this.instance != null) {
            if (InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
                context.objectProcessManager.scheduleProcessInstanceAsync(
                        InstanceConstants.PROCESS_START, instance, null);
            }
        }
        return this;
    }

    @Override
    public boolean createNew() {
        return this.hostMap == null;
    }

    @Override
    public DeploymentUnitInstance waitForStart() {
        this.hostMap = context.resourceMonitor.waitFor(this.hostMap, new ResourcePredicate<LoadBalancerHostMap>() {
            @Override
            public boolean evaluate(LoadBalancerHostMap obj) {
                return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
            }
        });
        this.instance = context.lbInstanceMgr.getLoadBalancerInstance(this.hostMap);
        return this;
    }
    
    @Override
    public boolean isStarted() {
        boolean mapActive = this.hostMap.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE);
        boolean instanceRunning = this.instance != null
                && this.instance.getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING);

        return mapActive && instanceRunning;
    }

    @Override
    public void waitForNotTransitioning() {
        if (this.hostMap != null) {
            this.hostMap = context.resourceMonitor.waitForNotTransitioning(this.hostMap);
        }
    }
}