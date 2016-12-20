package io.cattle.platform.app;

import io.cattle.platform.metrics.util.MetricsStartup;
import io.cattle.platform.metrics.util.MetricsUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;


@Configuration
public class MetricsConfig {

    @Bean
    MetricsStartup MetricsStartup() {
        return new MetricsStartup();
    }

    @Bean
    MetricRegistry MetricsRegistry() {
        return MetricsUtil.getRegistry();
    }

    @Bean
    HealthCheckRegistry HealthCheckRegistry() {
        return MetricsUtil.getHealthCheckRegistry();
    }

}