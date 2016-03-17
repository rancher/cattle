package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceLock extends AbstractBlockingLockDefintion {

    public ServiceLock(Service service) {
        super("SERVICE." + service.getId());
    }

}
