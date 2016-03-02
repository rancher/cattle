package io.cattle.platform.core.constants;

public class HealthcheckConstants {

    public static final String HEALTH_STATE_HEALTHY = "healthy";
    public static final String HEALTH_STATE_UPDATING_HEALTHY = "updating-healthy";
    public static final String HEALTH_STATE_UNHEALTHY = "unhealthy";
    public static final String HEALTH_STATE_UPDATING_UNHEALTHY = "updating-unhealthy";
    public static final String HEALTH_STATE_INITIALIZING = "initializing";
    public static final String HEALTH_STATE_REINITIALIZING = "reinitializing";

    public static final String SERVICE_HEALTH_STATE_DEGRADED = "degraded";
    public static final String SERVICE_HEALTH_STATE_STARTED_ONCE = "started-once";

    public static final String PROCESS_UPDATE_HEALTHY = "instance.updatehealthy";
    public static final String PROCESS_UPDATE_UNHEALTHY = "instance.updateunhealthy";
    public static final String PROCESS_UPDATE_REINITIALIZING = "instance.updatereinitializing";

    public static boolean isInit(String state) {
        return state != null && state.equalsIgnoreCase(HEALTH_STATE_INITIALIZING)
                || state.equalsIgnoreCase(HEALTH_STATE_REINITIALIZING);
    }

}
