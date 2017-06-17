package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.inator.util.StateUtil;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Date;

public interface BasicStateWrapper {

    default boolean isTransitioning() {
        return getMetadataManager().isTransitioningState(Instance.class, getState());
    }

    default boolean isActive() {
        // TODO: check start-once, restart policy, etc...
        return StateUtil.isActive(getState());
    }

    default boolean isInactive() {
        if (getRemoved() != null) {
            return true;
        }
        return StateUtil.isInactive(getState());
    }

    default boolean isHealthy() {
        // TODO: check start-once, restart policy, etc...
        return StateUtil.isHealthy(getHealthState());
    }

    default boolean isUnhealthy() {
        // TODO: check start-once, restart policy, etc...
        return StateUtil.isUnhealthy(getHealthState());
    }

    default boolean isError() {
        return CommonStatesConstants.ERROR.equals(getState());
    }

    void create();

    void activate();

    void deactivate();

    boolean remove();

    String getState();

    String getHealthState();

    Date getRemoved();

    ObjectMetaDataManager getMetadataManager();


}
