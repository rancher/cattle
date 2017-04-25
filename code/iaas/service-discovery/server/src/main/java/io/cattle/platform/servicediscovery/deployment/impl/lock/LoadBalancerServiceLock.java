package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class LoadBalancerServiceLock extends AbstractLockDefinition {

    public LoadBalancerServiceLock(long serviceId) {
        super("LBSERVICE.CONFIG.UPDATE." + serviceId);
    }
}
