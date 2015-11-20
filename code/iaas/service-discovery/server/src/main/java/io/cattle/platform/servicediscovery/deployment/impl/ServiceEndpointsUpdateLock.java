package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ServiceEndpointsUpdateLock extends AbstractBlockingLockDefintion {

    public ServiceEndpointsUpdateLock(Service service) {
        super("SERVICE." + service.getId() + "ENDPOINTS.UPDATE");
    }
}
