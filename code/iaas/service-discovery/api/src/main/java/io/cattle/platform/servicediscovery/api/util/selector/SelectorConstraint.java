package io.cattle.platform.servicediscovery.api.util.selector;

import java.util.Map;

public abstract class SelectorConstraint<T> {
    public enum Op {
        NEQ("!="),
        EQ("="),
        NOTIN(" notin "),
        IN(" in "),
        NOOP("");

        String selectorSymbol;

        private Op(String selectorSymbol) {
            this.selectorSymbol = selectorSymbol;
        }

        public String getSelectorSymbol() {
            return selectorSymbol;
        }
    }

    protected String key;
    protected T value;

    public SelectorConstraint(String key, T value) {
        super();
        this.key = key;
        this.value = value;
    }

    public abstract boolean isMatch(Map<String, String> toCompare);

    public T getValue() {
        return value;
    }
}
