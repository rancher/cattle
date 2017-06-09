package io.cattle.platform.loop.trigger;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.ProcessInstance;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.inator.InatorLifecycleManager;
import io.cattle.platform.loop.LoopFactoryImpl;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitReconcileTrigger implements Trigger {

    @Inject
    InatorLifecycleManager lifecycleManager;
    @Inject
    LoopManager loopManager;

    @Override
    public void trigger(ProcessInstance process) {
        Object resource = process.getResource();
        if (resource == null) {
            return;
        }
        for (Long id : lifecycleManager.impacetedDeploymentUnitUpdate(resource)) {
            System.err.println("KICKING DU FROM " + process.getRef().getResourceKey());
            loopManager.kick(LoopFactoryImpl.DU_RECONCILE, ServiceConstants.KIND_DEPLOYMENT_UNIT, id, resource);
        }
    }

}