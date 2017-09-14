package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.impl.OptionCallback;
import io.cattle.platform.api.auth.impl.PolicyOptions;

import java.util.HashMap;
import java.util.Map;

public class PolicyOptionsWrapper implements PolicyOptions {

    PolicyOptions options;
    Map<String, String> values = new HashMap<String, String>();
    Map<String, OptionCallback> callbacks = new HashMap<String, OptionCallback>();

    public PolicyOptionsWrapper(PolicyOptions options) {
        super();
        this.options = options;
    }

    @Override
    public boolean isOption(String optionName) {
        return Boolean.parseBoolean(getOption(optionName));
    }

    @Override
    public String getOption(String optionName) {
        OptionCallback callback = callbacks.get(optionName);
        if (callback != null) {
            return callback.getOption();
        }
        String value = values.get(optionName);
        return value == null ? options.getOption(optionName) : value;
    }

    @Override
    public void setOption(String name, String value) {
        values.put(name, value);
    }

    public void addCallback(String name, OptionCallback callback) {
        callbacks.put(name, callback);
    }
}
