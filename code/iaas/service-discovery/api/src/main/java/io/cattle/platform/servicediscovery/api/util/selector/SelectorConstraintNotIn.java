package io.cattle.platform.servicediscovery.api.util.selector;

import java.util.List;
import java.util.Map;

public class SelectorConstraintNotIn extends SelectorConstraint<List<String>> {
    public SelectorConstraintNotIn(String key, List<String> value) {
        super(key, value);
    }

    @Override
    public boolean isMatch(Map<String, String> toCompare) {
        boolean found = false;
        for (String key : toCompare.keySet()) {
            if (this.key.equalsIgnoreCase(key)) {
                if (!this.getValue().contains(toCompare.get(key).toLowerCase())) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
}
