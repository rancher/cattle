package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

public interface RancherConfigToComposeFormatter {
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform);
}
