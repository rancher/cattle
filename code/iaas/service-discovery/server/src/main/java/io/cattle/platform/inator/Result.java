package io.cattle.platform.inator;

import io.cattle.platform.inator.Unit.UnitState;

public class Result {
    Unit.UnitState state;
    Unit unit;

    public Result(UnitState state, Unit unit) {
        super();
        this.state = state;
        this.unit = unit;
    }

    public static Result good() {
        return new Result(UnitState.GOOD, null);
    }

    public void aggregate(Result result) {
        if (result.state.ordinal() > this.state.ordinal()) {
            this.state = result.state;
            this.unit = result.unit;
        }
    }

    public Unit.UnitState getState() {
        return state;
    }

}
