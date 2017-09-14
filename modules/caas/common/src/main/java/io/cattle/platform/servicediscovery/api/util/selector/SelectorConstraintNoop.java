package io.cattle.platform.servicediscovery.api.util.selector;

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
