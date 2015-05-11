package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RancherLoadBalancerConfigToComposeFormatter implements RancherConfigToComposeFormatter {

    @Override
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!item.getDockerName().equalsIgnoreCase(ServiceDiscoveryConfigItem.LB_CONGFIG.getDockerName())) {
            return null;
        }
        valueToTransform = lowerCaseParameters(valueToTransform);
        return valueToTransform;
    }

    @SuppressWarnings("unchecked")
    private Object lowerCaseParameters(Object valueToTransform) {
        // lower case all the parameters
        if (valueToTransform instanceof Map) {
            Map<String, Object> map = (Map<String, Object>)valueToTransform;
            Iterator<String> it = map.keySet().iterator();
            Map<String, Object> newMap = new HashMap<>();
            while (it.hasNext()) {
                String key = it.next();
                if (map.get(key) instanceof Map) {
                    lowerCaseParameters(map.get(key));
                }

                newMap.put(key.toLowerCase(), map.get(key));
                it.remove();
            }
            map.putAll(newMap);
        }

        return valueToTransform;
    }
}
