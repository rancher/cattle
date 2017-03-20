package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ServiceLinkLock extends AbstractLockDefinition {

    public ServiceLinkLock(long serviceId, long targetServiceId) {
        super("SERVICE." + serviceId + ".SERVICE." + targetServiceId);
    }
}
