package io.cattle.platform.inator;

import java.util.Collection;

public interface Unit {

    enum UnitState {
        GOOD,
        WAITING,
        ERROR
    }

    Result scheduleActions(InatorContext context);

    Result define(InatorContext context, boolean desired);

    Collection<UnitRef> dependencies(InatorContext context);

    UnitRef getRef();

    Result remove(InatorContext context);

    String getDisplayName();

}
