package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class PSKConfigItemFactory implements ConfigItemFactory {

    @Inject
    NicDao nicDao;
    @Inject
    DataDao dataDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    ConfigItemStatusManager versionManager;

    @Override
    public Collection<ConfigItem> getConfigItems() throws IOException {
        return Arrays.<ConfigItem>asList(new PSKConfigItem(objectManager, nicDao, dataDao, versionManager));
    }

}
