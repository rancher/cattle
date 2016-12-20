package io.cattle.platform.api.resource;

import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.request.resource.impl.ResourceManagerLocatorImpl;

public class ExtensionResourceManagerLocator extends ResourceManagerLocatorImpl implements InitializationTask {

    @Override
    public void start() {
        init();
    }

}
