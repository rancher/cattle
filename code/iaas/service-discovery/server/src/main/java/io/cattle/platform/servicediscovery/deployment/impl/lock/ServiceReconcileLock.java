package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceReconcileLock extends AbstractBlockingLockDefintion {

    public ServiceReconcileLock(Service service) {
        super("SERVICE.RECONCILE." + service.getId());
    }
}
