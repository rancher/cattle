package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
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
    public UnitState scheduleActions(InatorContext context) {
        return UnitState.ERROR;
    }

    @Override
    public void define(InatorContext context) {
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
    public boolean remove() {
        return true;
    }

}
