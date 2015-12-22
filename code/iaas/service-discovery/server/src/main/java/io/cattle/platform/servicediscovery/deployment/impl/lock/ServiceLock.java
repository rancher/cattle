package io.cattle.platform.servicediscovery.deployment.impl.lock;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ServiceLock extends AbstractLockDefinition {

    public ServiceLock(Service service) {
        super("SERVICE." + service.getId());
    }

}
