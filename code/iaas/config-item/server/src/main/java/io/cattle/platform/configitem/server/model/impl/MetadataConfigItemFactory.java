package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.context.impl.ServiceMetadataInfoFactory;
import io.cattle.platform.configitem.registry.ConfigItemRegistry;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class MetadataConfigItemFactory implements ConfigItemFactory, InitializationTask {

    @Inject
    ServiceMetadataInfoFactory factory;
    @Inject
    ObjectManager objectManager;
    @Inject
    ConfigItemStatusManager versionManager;
    @Inject
    ConfigItemStatusDao statusDao;
    @Inject
    ConfigItemRegistry itemRegistry;


    @Override
    public Collection<ConfigItem> getConfigItems() throws IOException {
        return Arrays.<ConfigItem>asList(new MetadataConfigItem(objectManager, factory, versionManager, itemRegistry));
    }

    @Override
    public void start() {
        MetadataConfigItem item;
        try {
            item = new MetadataConfigItem(objectManager, factory, versionManager, itemRegistry);
        } catch (IOException e) {
            throw new IllegalStateException("Reseting metadata config item", e);
        }
        statusDao.reset(item.getName(), item.getSourceRevision());
    }

}
