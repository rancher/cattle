package io.cattle.platform.servicediscovery.api.service.impl;

import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.service.RancherConfigToComposeFormatter;

import javax.inject.Named;

@Named
public class RancherImageToComposeFormatter implements RancherConfigToComposeFormatter {
    private static final String IMAGE_PREFIX = "docker:";

    @Override
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!item.getDockerName().equalsIgnoreCase(ServiceDiscoveryConfigItem.IMAGE.getDockerName())) {
            return null;
        }
        if (valueToTransform.toString().startsWith(IMAGE_PREFIX)) {
            valueToTransform = valueToTransform.toString().replaceFirst(IMAGE_PREFIX, "");
        }
        return valueToTransform;
    }

}
