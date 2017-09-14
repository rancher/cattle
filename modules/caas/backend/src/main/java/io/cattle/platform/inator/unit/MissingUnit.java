package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;

import java.util.Collection;
import java.util.Collections;

public class MissingUnit implements Unit {

    UnitRef ref;

    public MissingUnit(UnitRef ref) {
        this.ref = ref;
    }

    @Override
    public Result scheduleActions(InatorContext context) {
        return new Result(UnitState.ERROR, this,
                String.format("Missing %s", getDisplayName()));
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        return Result.good();
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return Collections.emptyList();
    }

    @Override
    public UnitRef getRef() {
        return ref;
    }

    @Override
    public Result remove(InatorContext context) {
        return Result.good();
    }

    @Override
    public String getDisplayName() {
        return ref.toString();
    }

}
