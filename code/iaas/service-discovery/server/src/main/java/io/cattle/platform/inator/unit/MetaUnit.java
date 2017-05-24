package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;

import java.util.Collection;

public class MetaUnit implements Unit {

    @Override
    public Result scheduleActions(InatorContext context) {
        return Result.good();
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        return Result.good();
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return context.getUnits().keySet();
    }

    @Override
    public UnitRef getRef() {
        return new UnitRef("*");
    }

    @Override
    public Result remove(InatorContext context) {
        return Result.good();
    }

    @Override
    public String getDisplayName() {
        return "meta";
    }

}
