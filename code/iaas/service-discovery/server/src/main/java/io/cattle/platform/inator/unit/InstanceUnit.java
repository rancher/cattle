package io.cattle.platform.inator.unit;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.HashSet;
import java.util.Set;

public class InstanceUnit implements Unit, BasicStateUnit {

    String name;
    LaunchConfig lc;
    InstanceWrapper instance;
    boolean cleanup;

    // Needed for create only
    StackWrapper stack;
    DeploymentUnitWrapper unit;

    public InstanceUnit(InstanceWrapper instance, boolean cleanup) {
        super();
        this.instance = instance;
        this.lc = instance.getLaunchConfig();
        this.name = this.lc.getName();
        this.cleanup = cleanup;
    }

    public InstanceUnit(String name, LaunchConfig lc, boolean cleanup, StackWrapper stack, DeploymentUnitWrapper unit) {
        this.name = name;
        this.lc = lc;
        this.stack = stack;
        this.unit = unit;
        this.cleanup = cleanup;
    }

    @Override
    public void define(InatorContext context) {
        if (instance == null) {
            instance = lc.create(context, stack, unit);
        }
    }

    @Override
    public Set<UnitRef> dependencies(InatorContext context) {
        Set<UnitRef> deps = new HashSet<>(lc.getDependenciesHolders().keySet());
        Set<UnitRef> desiredSet = context.getInator().getDesiredUnits();
        UnitRef selfRef = getRef();

        for (UnitRef otherRef : context.getUnits().keySet()) {
            if (!isSameLaunchConfig(selfRef)) {
                continue;
            }

            if (desiredSet.contains(selfRef)) {
                // We are replacing a different container
                if (!lc.isStartFirst()) {
                    deps.add(otherRef);
                }
            } else {
                // We are being replaced
                if (lc.isStartFirst()) {
                    deps.add(otherRef);
                }
            }
        }

        return lc.getDependenciesHolders().keySet();
    }

    @Override
    public UnitRef getRef() {
        return new UnitRef(lc.getRevision() + "/" + name);
    }

    protected boolean isSameLaunchConfig(UnitRef unitRef) {
        String[] parts = unitRef.toString().split("/");
        return parts.length == 2 && name.equals(parts[1]) && !unitRef.toString().equals(getRef().toString());
    }

    @Override
    public BasicStateWrapper getWrapper() {
        return instance;
    }

    @Override
    public UnitState removeBad(InatorContext context) {
        if (!lc.hasQuorum() || cleanup) {
            remove();
            return UnitState.WAITING;
        }
        return UnitState.BAD;
    }

    public Object getInstanceId() {
        return instance.getId();
    }

}