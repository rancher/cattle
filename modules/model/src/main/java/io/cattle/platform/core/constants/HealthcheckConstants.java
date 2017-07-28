package io.cattle.platform.core.constants;

public class HealthcheckConstants {

    public static final String HEALTH_STATE_HEALTHY = "healthy";
    public static final String HEALTH_STATE_UNHEALTHY = "unhealthy";
    public static final String HEALTH_STATE_INITIALIZING = "initializing";
    public static final String SERVICE_HEALTH_STATE_DEGRADED = "degraded";

    public static boolean isHealthy(String healthState) {
        return healthState == null || HEALTH_STATE_HEALTHY.equals(healthState);
    }
}
