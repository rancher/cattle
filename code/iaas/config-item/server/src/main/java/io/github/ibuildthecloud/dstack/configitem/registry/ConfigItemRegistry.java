package io.github.ibuildthecloud.dstack.configitem.registry;

import io.github.ibuildthecloud.dstack.configitem.server.model.ConfigItem;

import java.util.Collection;

public interface ConfigItemRegistry {

    ConfigItem getConfigItem(String name);

    Collection<ConfigItem> getConfigItems();

}
