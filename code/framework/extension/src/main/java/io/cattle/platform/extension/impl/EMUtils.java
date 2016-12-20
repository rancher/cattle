package io.cattle.platform.extension.impl;

import io.cattle.platform.util.type.NamedUtils;

import org.apache.commons.configuration.AbstractConfiguration;

public class EMUtils {

    public static AbstractConfiguration addConfig(ExtensionManagerImpl em, AbstractConfiguration config, String name) {
        em.addObject("config", AbstractConfiguration.class, config, name);
        return config;
    }

    public static <T> T add(ExtensionManagerImpl em, Class<?> clz, T t, String name) {
        String key = NamedUtils.toDotSeparated(clz.getSimpleName());
        em.addObject(key, clz, t, name);
        return t;
    }

    public static <T> T add(ExtensionManagerImpl em, Class<?> clz, T t) {
        return add(em, clz, t, NamedUtils.getName(t));
    }

}
