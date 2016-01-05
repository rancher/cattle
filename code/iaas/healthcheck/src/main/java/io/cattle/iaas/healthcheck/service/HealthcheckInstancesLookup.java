package io.cattle.iaas.healthcheck.service;

import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface HealthcheckInstancesLookup {

    List<? extends Instance> getInstances(Object obj);

}
