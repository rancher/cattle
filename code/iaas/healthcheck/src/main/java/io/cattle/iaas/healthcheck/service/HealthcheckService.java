package io.cattle.iaas.healthcheck.service;

import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;

public interface HealthcheckService {

    // only cattle instance type is supported at the moment
    // might add support for ip address later
    enum HealthcheckInstanceType {
        INSTANCE
    }
    
    void updateHealthcheck(String healthcheckInstanceHostMapUuid, final long externalTimestamp, final String health);

    void registerForHealtcheck(HealthcheckInstanceType instanceType, long id);

    void updateInstanceHealthState(Instance instance, String updateWithState);

    void healthCheckReconcile(HealthcheckInstanceHostMap hcihm, String healthState);
}
