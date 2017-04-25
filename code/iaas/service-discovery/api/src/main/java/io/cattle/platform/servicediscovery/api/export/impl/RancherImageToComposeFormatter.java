package io.cattle.platform.servicediscovery.api.export.impl;

import javax.inject.Named;

@Named
public class RancherImageToComposeFormatter implements RancherConfigToComposeFormatter {
    private static final String IMAGE_PREFIX = "docker:";

    @Override
    public Object format(ComposeExportConfigItem item, Object valueToTransform) {
        if (!item.getDockerName().equalsIgnoreCase(ComposeExportConfigItem.IMAGE.getDockerName())) {
            return null;
        }
        if (valueToTransform.toString().startsWith(IMAGE_PREFIX)) {
            valueToTransform = valueToTransform.toString().replaceFirst(IMAGE_PREFIX, "");
        }
        return valueToTransform;
    }

}
