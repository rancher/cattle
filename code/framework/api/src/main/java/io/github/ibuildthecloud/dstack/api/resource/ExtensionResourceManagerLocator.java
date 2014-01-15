package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.request.resource.impl.ResourceManagerLocatorImpl;

public class ExtensionResourceManagerLocator extends ResourceManagerLocatorImpl implements InitializationTask {

    @Override
    public void start() {
        init();
    }

    @Override
    public void stop() {
    }

}
