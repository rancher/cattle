package io.cattle.platform.configitem.registry;

import io.cattle.platform.configitem.server.model.ConfigItem;

import java.util.Collection;

public interface ConfigItemRegistry {

    ConfigItem getConfigItem(String name);

    Collection<ConfigItem> getConfigItems();

}
