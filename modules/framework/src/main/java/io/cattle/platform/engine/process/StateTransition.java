package io.cattle.platform.engine.process;

public class StateTransition {

    public enum Style {
        TRANSITIONING, DONE
    };

    String fromState, toState, field;
    Style type;

    public StateTransition() {
    }

    public StateTransition(String fromState, String toState, String field, Style type) {
        super();
        this.fromState = fromState;
        this.toState = toState;
        this.field = field;
        this.type = type;
    }

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }

    public Style getType() {
        return type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

}
