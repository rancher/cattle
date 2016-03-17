package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceInstanceLock extends AbstractBlockingLockDefintion {

    public ServiceInstanceLock(Service service, Instance instance) {
        super("SERVICE." + service.getId() + ".INSTANCE." + instance.getId());
    }
}
