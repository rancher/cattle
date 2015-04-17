package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

public class RancherImageToComposeFormatter implements RancherConfigToComposeFormatter {
    private static final String IMAGE_PREFIX = "docker:";

    @Override
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!item.getComposeName().equalsIgnoreCase(ServiceDiscoveryConfigItem.IMAGE.getComposeName())) {
            return null;
        }
        if (valueToTransform.toString().startsWith(IMAGE_PREFIX)) {
            valueToTransform = valueToTransform.toString().replaceFirst(IMAGE_PREFIX, "");
        }
        return valueToTransform;
    }

}
