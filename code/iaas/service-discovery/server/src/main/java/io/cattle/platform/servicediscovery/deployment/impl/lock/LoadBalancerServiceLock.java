package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class LoadBalancerServiceLock extends AbstractBlockingLockDefintion {

    public LoadBalancerServiceLock(long serviceId) {
        super("LBSERVICE.CONFIG.UPDATE." + serviceId);
    }
}
