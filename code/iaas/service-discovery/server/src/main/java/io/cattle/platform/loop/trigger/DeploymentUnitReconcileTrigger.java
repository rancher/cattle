package io.cattle.platform.loop.trigger;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.servicediscovery.deployment.lookups.DeploymentUnitLookup;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitReconcileTrigger implements Trigger {

    @Inject
    LoopManager loopManager;
    @Inject
    List<DeploymentUnitLookup> deploymentUnitLookups;

    @Override
    public void trigger(ProcessInstance process) {
        Object resource = process.getResource();
        if (resource == null) {
            return;
        }

        Set<Long> deploymentUnitIds = new HashSet<>();
        for (DeploymentUnitLookup lookup : deploymentUnitLookups) {
            Collection<Long> deploymentUnits = lookup.getDeploymentUnits(resource);
            if (deploymentUnits != null) {
                deploymentUnitIds.addAll(deploymentUnits);
            }
        }

        for (Long id : deploymentUnitIds) {
            loopManager.kick(LoopFactoryImpl.DU_RECONCILE, ServiceConstants.KIND_DEPLOYMENT_UNIT, id, resource);
        }
    }

}