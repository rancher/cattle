package io.cattle.platform.inator;

import io.cattle.platform.inator.Unit.UnitState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Result {
    Unit.UnitState state;
    Unit unit;
    String reason;
    String logLevel = "info";
    List<Result> results;

    public Result(UnitState state, Unit unit, String reason) {
        super();
        this.state = state;
        this.unit = unit;
        this.reason = reason;
    }

    public static Result good() {
        return new Result(UnitState.GOOD, null, null);
    }

    public boolean isGood() {
        return state == UnitState.GOOD;
    }

    public void aggregate(Result result) {
        if (result == null) {
            return;
        }

        if (result.state.ordinal() > this.state.ordinal()) {
            this.state = result.state;
            this.unit = result.unit;
        }
        if (results == null) {
            results = new ArrayList<>();
        }
        results.add(result);
    }

    public Unit.UnitState getState() {
        return state;
    }

    public List<Result> getResults() {
        return results == null ? Collections.emptyList() : results;
    }

    public Unit getUnit() {
        return unit;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getReason() {
        return reason;
    }

}
