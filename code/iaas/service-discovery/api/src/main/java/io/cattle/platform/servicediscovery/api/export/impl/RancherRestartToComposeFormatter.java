package io.cattle.platform.servicediscovery.api.export.impl;

import io.cattle.platform.json.JsonMapper;

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
