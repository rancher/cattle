package io.cattle.platform.inator.unit;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;

public interface BasicStateUnit extends Unit {

    @Override
    default UnitState scheduleActions(InatorContext context) {
        switch (context.getInator().getDesiredState()) {
        case INACTIVE:
            return deactivate(context);
        case ACTIVE:
            return activate(context);
        default:
            throw new IllegalStateException("Invalid desired state: " + context.getInator().getDesiredState());
        }
    }


    BasicStateWrapper getWrapper();

    default UnitState activate(InatorContext context) {
        BasicStateWrapper wrapper = getWrapper();

        if (wrapper.isTransitioning()) {
            return UnitState.WAITING;
        }

        if (wrapper.isActive()) {
            if (wrapper.isHealthy()) {
                return UnitState.GOOD;
            } else {
                return removeBad(context);
            }
        }

        if (CommonStatesConstants.REQUESTED.equals(wrapper.getState())) {
            wrapper.create();
        } else {
            wrapper.activate();
        }
        return UnitState.WAITING;
    }

    UnitState removeBad(InatorContext context);

    default UnitState deactivate(InatorContext context) {
        BasicStateWrapper wrapper = getWrapper();
        // For deactivate it is possible that the instance doesn't exist yet
        if (wrapper == null || wrapper.isInactive()) {
            return  UnitState.GOOD;
        }

        wrapper.deactivate();
        return UnitState.WAITING;
    }

    @Override
    default boolean remove() {
        BasicStateWrapper wrapper = getWrapper();
        if (wrapper != null) {
            return wrapper.remove();
        }
        return true;
    }

}