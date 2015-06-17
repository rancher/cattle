package io.cattle.platform.servicediscovery.api.service;

import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;

public interface RancherConfigToComposeFormatter {
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform);
}
