package io.cattle.platform.inator.unit;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;

public interface BasicStateUnit extends Unit {

    public enum RemoveReason { ERROR, UNHEALTHY, OTHER };

    @Override
    default Result scheduleActions(InatorContext context) {
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

    default Result activate(InatorContext context) {
        BasicStateWrapper wrapper = getWrapper();

        if (wrapper.isTransitioning()) {
            return new Result(UnitState.WAITING, this, String.format("Waiting for transitioning %s [%s]", getDisplayName(), wrapper.getState()));
        }

        if (wrapper.isActive()) {
            if (wrapper.isHealthy()) {
                return Result.good();
            } else if (wrapper.isUnhealthy()) {
                return removeBad(context, RemoveReason.UNHEALTHY);
            } else {
                return new Result(UnitState.WAITING, this, String.format("Waiting for %s to become healthy [%s][%s]", getDisplayName(), wrapper.getState(),
                        wrapper.getHealthState()));
            }
        }

        if (wrapper.isError()) {
            return removeBad(context, RemoveReason.ERROR);
        }

        if (CommonStatesConstants.REQUESTED.equals(wrapper.getState())) {
            wrapper.create();
            return new Result(UnitState.WAITING, this, String.format("Creating %s", getDisplayName()));
        } else {
            wrapper.activate();
            return new Result(UnitState.WAITING, this, String.format("Activating %s", getDisplayName()));
        }
    }

    Result removeBad(InatorContext context, RemoveReason reason);

    default Result deactivate(InatorContext context) {
        BasicStateWrapper wrapper = getWrapper();
        // For deactivate it is possible that the instance doesn't exist yet
        if (wrapper == null || wrapper.isInactive()) {
            return Result.good();
        }

        try {
            wrapper.deactivate();
        } catch (ProcessCancelException e) {
            return new Result(UnitState.WAITING, this, String.format("Can not deactivate %s right now: %s", getDisplayName(), e.getMessage()));
        }
        return new Result(UnitState.WAITING, this, String.format("Deactivating %s", getDisplayName()));
    }

    @Override
    default Result remove(InatorContext context) {
        BasicStateWrapper wrapper = getWrapper();
        if (wrapper != null) {
            if (wrapper.remove()) {
                return Result.good();
            } else {
                return new Result(UnitState.WAITING, this, String.format("Removing %s", getDisplayName()));
            }
        }
        return Result.good();
    }

}