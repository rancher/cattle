package io.cattle.platform.inator.util;

import static io.cattle.platform.util.type.CollectionUtils.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.Set;

public class StateUtil {

    private static Set<String> PAUSED_LIKE = set(
            ServiceConstants.STATE_PAUSED,
            ServiceConstants.STATE_PAUSING);
    private static Set<String> INACTIVE_LIKE = set(
            CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.INACTIVE,
            CommonStatesConstants.UPDATING_INACTIVE);
    private static Set<String> REMOVED_LIKE = set(
            CommonStatesConstants.REMOVING,
            CommonStatesConstants.REMOVED);

    private static Set<String> ACTIVE =
            CollectionUtils.set(CommonStatesConstants.ACTIVE, InstanceConstants.STATE_RUNNING);
    private static Set<String> INACTIVE =
            CollectionUtils.set(CommonStatesConstants.INACTIVE, InstanceConstants.STATE_STOPPED);

    public static Inator.DesiredState getDesiredState(String state, Date removed) {
        if (removed != null || REMOVED_LIKE.contains(state)) {
            return DesiredState.REMOVED;
        }

        if (INACTIVE_LIKE.contains(state)) {
            return DesiredState.INACTIVE;
        }

        if (PAUSED_LIKE.contains(state)) {
            return DesiredState.NONE;
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

}