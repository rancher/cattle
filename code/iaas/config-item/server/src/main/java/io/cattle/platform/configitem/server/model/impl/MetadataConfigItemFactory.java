package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.context.impl.ServiceMetadataInfoFactory;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class MetadataConfigItemFactory implements ConfigItemFactory {

    @Inject
    ServiceMetadataInfoFactory factory;
    @Inject
    ObjectManager objectManager;
    @Inject
    ConfigItemStatusManager versionManager;

    @Override
    public Collection<ConfigItem> getConfigItems() throws IOException {
        return Arrays.<ConfigItem>asList(new MetadataConfigItem(objectManager, factory, versionManager));
    }

}
