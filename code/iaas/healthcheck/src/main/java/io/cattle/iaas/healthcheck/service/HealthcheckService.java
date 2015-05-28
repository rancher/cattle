package io.cattle.iaas.healthcheck.service;


public interface HealthcheckService {

    // only cattle instance type is supported at the moment
    // might add support for ip address later
    public enum HealthcheckInstanceType {
        INSTANCE
    }
    
    public static final String HEALTH_STATE_HEALTHY = "healthy";
    public static final String HEALTH_STATE_UPDATING_HEALTHY = "updating-healthy";
    public static final String HEALTH_STATE_UNHEALTHY = "unhealthy";
    public static final String HEALTH_STATE_UPDATING_UNHEALTHY = "updating-unhealthy";

    public void updateHealthcheck(HealthcheckInstanceType instanceType, long instanceId, boolean healthy);
    
    public void registerForHealtcheck(HealthcheckInstanceType instanceType, long instanceId);
}
