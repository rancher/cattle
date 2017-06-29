package io.cattle.platform.loadbalancer.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class LoadBalancerServiceLock extends AbstractBlockingLockDefintion {

    public LoadBalancerServiceLock(long serviceId) {
        super("LBSERVICE.CONFIG.UPDATE." + serviceId);
    }
}
