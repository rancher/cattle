package io.cattle.platform.api.auth.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class ArchaiusPolicyOptions implements PolicyOptions {

    private static String PROP_FORMAT = "account.type.%s.%s";

    Map<String, DynamicBooleanProperty> bools = new ConcurrentHashMap<String, DynamicBooleanProperty>();
    Map<String, DynamicStringProperty> strings = new ConcurrentHashMap<String, DynamicStringProperty>();
    Map<String, OptionCallback> callbacks = new HashMap<String, OptionCallback>();
    String name;

    public ArchaiusPolicyOptions(String name) {
        this.name = name;
    }

    @Override
    public boolean isOption(String optionName) {
        DynamicBooleanProperty prop = bools.get(optionName);
        if (prop == null) {
            prop = ArchaiusUtil.getBoolean(String.format(PROP_FORMAT, name, optionName));
            bools.put(optionName, prop);
        }
        return prop.get();
    }

    @Override
    public String getOption(String optionName) {
        OptionCallback callback = callbacks.get(optionName);
        if (callback != null) {
            return callback.getOption();
        }

        DynamicStringProperty prop = strings.get(optionName);
        if (prop == null) {
            prop = ArchaiusUtil.getString(String.format(PROP_FORMAT, name, optionName));
            strings.put(optionName, prop);
        }
        return prop.get();
    }

    @Override
    public void setOption(String name, String value) {
        throw new UnsupportedOperationException();
    }

}
