package io.cattle.platform.servicediscovery.process.lock;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceDiscoveryServiceSetLinksLock extends AbstractBlockingLockDefintion {
    public ServiceDiscoveryServiceSetLinksLock(Service service) {
        super("SERVICE.SETLINKS." + service.getId());
    }
}
