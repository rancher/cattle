package io.cattle.platform.compose.export.impl;

public class RancherImageToComposeFormatter implements RancherConfigToComposeFormatter {

    @Override
    public Object format(ComposeExportConfigItem item, Object valueToTransform) {
        if (!item.getDockerName().equalsIgnoreCase(ComposeExportConfigItem.IMAGE.getDockerName())) {
            return null;
        }
        return valueToTransform;
    }

}
