package io.cattle.platform.servicediscovery.selector.impl;

import io.cattle.platform.servicediscovery.selector.SelectorConstraint;

import java.util.Map;

public class SelectorConstraintNoop extends SelectorConstraint<String> {

    public SelectorConstraintNoop(String key, String value) {
        super(key, value);
    }

    @Override
    public boolean isMatch(Map<String, String> toCompare) {
        return toCompare.containsKey(key);
    }

}
