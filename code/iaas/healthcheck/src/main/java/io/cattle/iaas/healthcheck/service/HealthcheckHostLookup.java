package io.cattle.iaas.healthcheck.service;

import io.cattle.platform.core.model.Host;

public interface HealthcheckHostLookup {
    public Host getHost(Object obj);
}
