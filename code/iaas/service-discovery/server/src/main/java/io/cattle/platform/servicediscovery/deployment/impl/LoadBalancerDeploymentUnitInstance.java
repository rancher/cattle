package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalancerDeploymentUnitInstance extends DeploymentUnitInstance {
    LoadBalancerHostMap hostMap;

    public LoadBalancerDeploymentUnitInstance() {
        super(null, null, null);
    }

    public LoadBalancerDeploymentUnitInstance(String uuid, Service service,
            LoadBalancerHostMap hostMap, DeploymentServiceContext context) {
        super(uuid, service, context);
        this.hostMap = hostMap;
        if (hostMap != null) {
            Instance instance = context.lbInstanceMgr.getLoadBalancerInstance(this.hostMap);
            if (instance != null) {
                this.instance = instance;
            }
        }
    }

    @Override
    public boolean isError() {
        if (this.hostMap.getState().equals(CommonStatesConstants.ACTIVE)) {
            if (this.instance == null || this.instance.getRemoved() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void remove() {
        context.objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_HOST_MAP_REMOVE,
                context.objectManager.reload(hostMap),
                null);
    }

    @Override
    public DeploymentUnitInstance start(List<Integer> volumesFromInstancesIds) {
        if (this.hostMap == null) {
            LoadBalancer lb = context.objectManager.findAny(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                    service.getId(),
                    LOAD_BALANCER.REMOVED, null);

            Map<String, Object> launchConfigData = context.sdService.buildLaunchData(service, this.labels, null,
                    volumesFromInstancesIds);
            Map<String, Object> launchConfig = new HashMap<>();
            launchConfig.put(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG, launchConfigData);
            this.hostMap = context.lbService.addHostWLaunchConfigToLoadBalancer(lb, launchConfig);
            this.instance = context.lbInstanceMgr.getLoadBalancerInstance(this.hostMap);
        } else if (this.instance != null) {
            if (InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
                DeferredUtils.nest(new Runnable() {
                    @Override
                    public void run() {
                        context.objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_START, instance,
                                null);
                    }
                });
            }
        }
        return this;
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
    public void stop() {
        if (this.instance != null) {
            context.objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance, null);
        }
    }

    @Override
    public boolean isStarted() {
        return this.hostMap.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE);
    }

    @Override
    public boolean needsCleanup() {
        // TODO implement when healthcheck comes in
        return false;
    }
}