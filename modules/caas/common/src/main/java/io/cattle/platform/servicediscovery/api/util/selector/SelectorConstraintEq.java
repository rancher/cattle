package io.cattle.platform.servicediscovery.api.util.selector;

import java.util.Map;

public class SelectorConstraintEq extends SelectorConstraint<String> {

    public SelectorConstraintEq(String key, String value) {
        super(key, value);
    }

    @Override
    public boolean isMatch(Map<String, String> toCompare) {
        boolean found = false;
        for (String key : toCompare.keySet()) {
            if (this.key.equalsIgnoreCase(key)) {
                if (this.value.equalsIgnoreCase(toCompare.get(key))) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

}
