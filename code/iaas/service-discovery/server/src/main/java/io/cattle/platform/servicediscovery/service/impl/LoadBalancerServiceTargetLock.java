package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class LoadBalancerServiceTargetLock extends AbstractLockDefinition {

    public LoadBalancerServiceTargetLock(Service service, ServiceExposeMap map) {
        super("LBSERVICE." + service.getId() + ".EXPOSEMAP." + map.getId());
    }
}
