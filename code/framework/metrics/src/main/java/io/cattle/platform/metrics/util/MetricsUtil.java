package io.cattle.platform.metrics.util;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

public class MetricsUtil {

    private static HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();
    private static MetricRegistry REGISTRY = new MetricRegistry();

    public static MetricRegistry getRegistry() {
        return REGISTRY;
    }

    public static void setRegistry(MetricRegistry registry) {
        REGISTRY = registry;
    }

    public static HealthCheckRegistry getHealthCheckRegistry() {
        return HEALTH_CHECK_REGISTRY;
    }

    public static void setHealthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
        HEALTH_CHECK_REGISTRY = healthCheckRegistry;
    }

}
