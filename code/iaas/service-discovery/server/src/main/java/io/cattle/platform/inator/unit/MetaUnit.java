package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;

import java.util.Collection;

public class MetaUnit implements Unit {

    @Override
    public UnitState scheduleActions(InatorContext context) {
        return UnitState.GOOD;
    }

    @Override
    public void define(InatorContext context) {
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
    public boolean remove() {
        return true;
    }

}
