package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;

import java.util.Collection;
import java.util.Collections;

public class WaitingUnit implements Unit {

    String message;

    public WaitingUnit(String message) {
        super();
        this.message = message;
    }

    @Override
    public Result scheduleActions(InatorContext context) {
        return waiting();
    }

    protected Result waiting() {
        return new Result(UnitState.WAITING, this, message);
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        return waiting();
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return Collections.emptyList();
    }

    @Override
    public UnitRef getRef() {
        return new UnitRef("waiting");
    }

    @Override
    public Result remove(InatorContext context) {
        return Result.good();
    }

    @Override
    public String getDisplayName() {
        return message;
    }

}
