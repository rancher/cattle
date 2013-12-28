package io.github.ibuildthecloud.dstack.engine.process;

public class StateTransition {

    public enum Style { TRANSITIONING, DONE };

    String fromState, toState;
    Style type;

    public StateTransition() {
    }

    public StateTransition(String fromState, String toState, Style type) {
        super();
        this.fromState = fromState;
        this.toState = toState;
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

}
