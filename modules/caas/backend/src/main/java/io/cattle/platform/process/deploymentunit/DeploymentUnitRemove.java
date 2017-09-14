package io.cattle.platform.process.deploymentunit;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.resource.pool.ResourcePoolManager;

public class DeploymentUnitRemove implements ProcessHandler {

    ResourcePoolManager resourcePoolManager;

    public DeploymentUnitRemove(ResourcePoolManager resourcePoolManager) {
        this.resourcePoolManager = resourcePoolManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        resourcePoolManager.releaseAllResources(state.getResource());
        return null;
    }

}
