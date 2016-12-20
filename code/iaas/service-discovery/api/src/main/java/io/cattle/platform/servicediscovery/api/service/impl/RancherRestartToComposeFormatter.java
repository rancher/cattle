package io.cattle.platform.servicediscovery.api.service.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.service.RancherConfigToComposeFormatter;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RancherRestartToComposeFormatter implements RancherConfigToComposeFormatter {
    @Inject
    JsonMapper jsonMapper;

    @Override
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!item.getDockerName().equalsIgnoreCase(ServiceDiscoveryConfigItem.RESTART.getDockerName())) {
            return null;
        }
        return Option.REMOVE;
    }
}
