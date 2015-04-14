package io.cattle.platform.lb.instance.process.lock;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class LoadBalancerInstancePortLock extends AbstractBlockingLockDefintion {
    public LoadBalancerInstancePortLock(Instance lbInstance) {
        super("LOADBALANCER.INSTANCE." + lbInstance.getId());
    }
}
