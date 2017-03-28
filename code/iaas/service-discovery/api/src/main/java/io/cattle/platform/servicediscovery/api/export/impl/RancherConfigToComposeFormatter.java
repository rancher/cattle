package io.cattle.platform.servicediscovery.api.export.impl;


public interface RancherConfigToComposeFormatter {
    public enum Option {
        REMOVE
    }
    public Object format(ComposeExportConfigItem item, Object valueToTransform);
}
