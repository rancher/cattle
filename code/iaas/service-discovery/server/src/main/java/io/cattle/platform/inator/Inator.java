package io.cattle.platform.inator;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Inator {

    enum DesiredState {
        NONE,
        ACTIVE,
        INACTIVE,
        REMOVED
    }

    List<Unit> collect();

    Map<UnitRef, Unit> fillIn(InatorContext context);

    Set<UnitRef> getDesiredUnits();

    DesiredState getDesiredState();
}