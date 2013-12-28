package io.github.ibuildthecloud.dstack.extension.spring;

import java.util.List;

public class ExtensionDiscoveryRegistry {

    List<ExtensionDiscovery> extensionDiscoveries;

    public List<ExtensionDiscovery> getExtensionDiscoveries() {
        return extensionDiscoveries;
    }

    public void setExtensionDiscoveries(List<ExtensionDiscovery> extensionDiscoveries) {
        this.extensionDiscoveries = extensionDiscoveries;
    }

}
