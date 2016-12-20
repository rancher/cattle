package io.cattle.platform.configitem.registry.impl;

import io.cattle.platform.configitem.registry.ConfigItemRegistry;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigItemRegistryImpl implements ConfigItemRegistry, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemRegistryImpl.class);

    Map<String, ConfigItem> items = new ConcurrentHashMap<String, ConfigItem>();
    List<ConfigItemFactory> factories = new ArrayList<ConfigItemFactory>();

    @Override
    public void start() {
        for (ConfigItemFactory factory : factories) {
            register(factory);
        }
    }

    public boolean register(ConfigItemFactory type) {
        try {
            for (ConfigItem item : type.getConfigItems()) {
                if (items.containsKey(item.getName()))
                    continue;

                log.info("Registering Config Item [{}]", item.getName());
                items.put(item.getName(), item);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get config items from factory [" + type + "]", e);
        }

        return true;
    }

    @Override
    public ConfigItem getConfigItem(String name) {
        return items.get(name);
    }

    @Override
    public Collection<ConfigItem> getConfigItems() {
        return items.values();
    }

    public List<ConfigItemFactory> getFactories() {
        return factories;
    }

    @Inject
    public void setFactories(List<ConfigItemFactory> factories) {
        this.factories = factories;
    }

}
