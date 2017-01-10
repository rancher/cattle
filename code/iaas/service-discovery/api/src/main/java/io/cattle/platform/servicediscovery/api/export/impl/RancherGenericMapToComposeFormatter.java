package io.cattle.platform.servicediscovery.api.export.impl;

import io.cattle.platform.util.type.NamedUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Named;

@Named
public class RancherGenericMapToComposeFormatter implements RancherConfigToComposeFormatter {

    @Override
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (!(item.getDockerName().equalsIgnoreCase(ServiceDiscoveryConfigItem.LB_CONGFIG.getDockerName()) || item
                .getDockerName().equals(ServiceDiscoveryConfigItem.HEALTHCHECK.getDockerName()))) {
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
                if (map.get(key) != null) {
                    newMap.put(NamedUtils.toUnderscoreSeparated(key), map.get(key));
                }
                it.remove();
            }
            map.putAll(newMap);
        }

        return valueToTransform;
    }
}
