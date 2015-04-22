package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

import java.util.Map;

import javax.inject.Inject;

public class RancherRestartToComposeFormatter implements RancherConfigToComposeFormatter {

    @Inject
    JsonMapper jsonMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!item.getComposeName().equalsIgnoreCase(ServiceDiscoveryConfigItem.RESTART.getComposeName())) {
            return null;
        }
        // transform object to string as thats how policy is defined in docker-compose
        Map<String, Object> map = (Map<String, Object>) valueToTransform;
        Object name = map.get("name");
        Object maxRetryCount = map.get("maximumRetryCount");
        if (maxRetryCount == null) {
            return name;
        }
        return name + ":" + maxRetryCount;
    }
}
