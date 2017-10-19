package io.cattle.platform.process.deploymentunit;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.resource.pool.ResourcePoolManager;

public class DeploymentUnitRemove implements ProcessHandler {

    ResourcePoolManager resourcePoolManager;
    LoadBalancerService loadBalancerService;

    public DeploymentUnitRemove(ResourcePoolManager resourcePoolManager,LoadBalancerService loadBalancerService ) {
        this.resourcePoolManager = resourcePoolManager;
        this.loadBalancerService = loadBalancerService;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        resourcePoolManager.releaseAllResources(state.getResource());
        loadBalancerService.removeFromLoadBalancerServices((DeploymentUnit)state.getResource());
        return null;
    }

}
