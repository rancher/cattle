package io.cattle.platform.core.constants;

public class HealthcheckConstants {

    public static final String HEALTH_STATE_HEALTHY = "healthy";
    public static final String HEALTH_STATE_UNHEALTHY = "unhealthy";

    public static final String HEALTH_STATE_INITIALIZING = "initializing";

    public static final String SERVICE_HEALTH_STATE_DEGRADED = "degraded";

    public static boolean isInit(String state) {
        return state != null && (state.equalsIgnoreCase(HEALTH_STATE_INITIALIZING)
                || state.equalsIgnoreCase(HEALTH_STATE_REINITIALIZING));
    }

    public static boolean isUnhealthy(String state) {
        return state != null && (state.equalsIgnoreCase(HEALTH_STATE_UNHEALTHY)
                || state.equalsIgnoreCase(HEALTH_STATE_UPDATING_UNHEALTHY));
    }

    public static boolean isHealthy(String state) {
        return state == null || state.equalsIgnoreCase(HEALTH_STATE_HEALTHY)
                || state.equalsIgnoreCase(HEALTH_STATE_UPDATING_HEALTHY);
    }
}
