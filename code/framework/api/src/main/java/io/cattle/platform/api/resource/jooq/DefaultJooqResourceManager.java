package io.cattle.platform.api.resource.jooq;

import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.request.resource.impl.ResourceManagerLocatorImpl;

import javax.inject.Inject;

public class DefaultJooqResourceManager extends AbstractJooqResourceManager {

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

    @Override
    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        super.setLocator(locator);
        if (locator instanceof ResourceManagerLocatorImpl) {
            ((ResourceManagerLocatorImpl) locator).setDefaultResourceManager(this);
        }
    }

}
