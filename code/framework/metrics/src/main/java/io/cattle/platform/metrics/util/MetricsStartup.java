package io.cattle.platform.metrics.util;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

public class MetricsStartup {

    JmxReporter reporter;

    @PostConstruct
    public void init() {
        MetricRegistry registry = MetricsUtil.getRegistry();

        reporter = JmxReporter.forRegistry(registry).build();
        reporter.start();
    }

    @PreDestroy
    public void destroy() {
        reporter.stop();
    }

}
