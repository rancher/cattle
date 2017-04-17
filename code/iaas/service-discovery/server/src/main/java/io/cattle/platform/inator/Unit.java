package io.cattle.platform.inator;

import java.util.Collection;

public interface Unit {

    enum UnitState {
        GOOD,
        WAITING,
        BAD,
        ERROR
    }

    UnitState scheduleActions(InatorContext context);

    void define(InatorContext context);

    Collection<UnitRef> dependencies(InatorContext context);

    UnitRef getRef();

    /**
     * @return True if removed, false if in progress
     */
    boolean remove();

}
