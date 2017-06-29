package io.cattle.platform.servicediscovery.api.lock;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ConvertToServiceLock extends AbstractBlockingLockDefintion {
    public ConvertToServiceLock(DeploymentUnit unit) {
        super("DEPLOYMENT.UNIT.CONVERT." + unit.getId());
    }
}
