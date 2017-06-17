package io.cattle.platform.compose.export.impl;


public interface RancherConfigToComposeFormatter {
    enum Option {
        REMOVE
    }
    Object format(ComposeExportConfigItem item, Object valueToTransform);
}
