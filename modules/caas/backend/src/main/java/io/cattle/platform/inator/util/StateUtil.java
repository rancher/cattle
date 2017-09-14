package io.cattle.platform.inator.util;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.Set;

import static io.cattle.platform.util.type.CollectionUtils.*;

public class StateUtil {

    private static Set<String> ERROR_LIKE = set(
            CommonStatesConstants.ERROR,
            CommonStatesConstants.ERRORING);
    private static Set<String> PAUSED_LIKE = set(
            ServiceConstants.STATE_PAUSED,
            ServiceConstants.STATE_PAUSING);
    private static Set<String> INACTIVE_LIKE = set(
            CommonStatesConstants.CREATING,
            CommonStatesConstants.INACTIVE,
            CommonStatesConstants.DEACTIVATING);
    private static Set<String> REMOVED_LIKE = set(
            CommonStatesConstants.REMOVING,
            CommonStatesConstants.REMOVED);

    private static Set<String> ACTIVE = CollectionUtils.set(
            CommonStatesConstants.ACTIVE,
            ServiceConstants.STATE_UPGRADED,
            InstanceConstants.STATE_RUNNING);
    private static Set<String> INACTIVE = CollectionUtils.set(
            CommonStatesConstants.INACTIVE,
            InstanceConstants.STATE_STOPPED,
            CommonStatesConstants.ERROR,
            CommonStatesConstants.REQUESTED);

    public static Inator.DesiredState getDesiredState(String state, Date removed) {
        if (removed != null || REMOVED_LIKE.contains(state)) {
            return DesiredState.REMOVED;
        }

        if (INACTIVE_LIKE.contains(state)) {
            return DesiredState.INACTIVE;
        }

        if (PAUSED_LIKE.contains(state)) {
            return DesiredState.PAUSE;
        }

        if (ERROR_LIKE.contains(state)) {
            return DesiredState.ERROR;
        }

        return DesiredState.ACTIVE;
    }

    public static boolean isActive(String state) {
        return ACTIVE.contains(state);
    }

    public static boolean isInactive(String state) {
        return INACTIVE.contains(state);
    }

    public static boolean isHealthy(String healthState) {
        return HealthcheckConstants.isHealthy(healthState);
    }

    public static boolean isUnhealthy(String healthState) {
        return HealthcheckConstants.HEALTH_STATE_UNHEALTHY.equals(healthState);
    }

    public static boolean isPaused(String state) {
        return PAUSED_LIKE.contains(state);
    }

}