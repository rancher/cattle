package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ServiceInstanceLock extends AbstractLockDefinition {

    public ServiceInstanceLock(Service service, Instance instance) {
        super("SERVICE." + service.getId() + ".INSTANCE." + instance.getId());
    }
}
