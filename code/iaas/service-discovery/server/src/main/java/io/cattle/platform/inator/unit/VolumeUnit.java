package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.wrapper.VolumeTemplateWrapper;
import io.cattle.platform.inator.wrapper.VolumeWrapper;

import java.util.Collection;

public class VolumeUnit implements Unit {

    public VolumeUnit(VolumeWrapper volume) {
        // TODO Auto-generated constructor stub
    }

    public VolumeUnit(String name, VolumeTemplateWrapper vt) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public UnitState scheduleActions(InatorContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void define(InatorContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UnitRef getRef() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean remove() {
        // TODO Auto-generated method stub
        return false;
    }

}
