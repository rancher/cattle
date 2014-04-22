package io.cattle.platform.object.meta;

import java.util.HashSet;
import java.util.Set;

public class ActionDefinition {

    Set<String> validStates = new HashSet<String>();

    public Set<String> getValidStates() {
        return validStates;
    }

    public void setValidStates(Set<String> validStates) {
        this.validStates = validStates;
    }
}
