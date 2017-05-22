package io.cattle.platform.inator.planner;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;

import java.util.Map;
import java.util.Set;

public interface UnitPlanner {

    Map<UnitRef, Unit> fillIn(InatorContext context);

    Set<UnitRef> getDesiredUnits();

}
