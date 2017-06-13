package io.cattle.platform.inator;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Inator {

    enum DesiredState {
        PAUSE,
        ERROR,
        ACTIVE,
        INACTIVE,
        REMOVED
    }

    List<Unit> collect();

    Map<UnitRef, Unit> fillIn(InatorContext context);

    Set<UnitRef> getDesiredRefs();

    DesiredState getDesiredState();

    Result postProcess(InatorContext context, Result result);
}