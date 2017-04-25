package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class DeploymentUnitLock extends AbstractLockDefinition {

    public DeploymentUnitLock(DeploymentUnit unit) {
        super("DEPLOYMENT.UNIT." + unit.getId());
    }
}
