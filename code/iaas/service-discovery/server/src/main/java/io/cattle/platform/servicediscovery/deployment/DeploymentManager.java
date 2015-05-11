package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;

import java.util.Map;

public interface DeploymentManager {

    void activate(Service service, Map<String, Object> data);

    void deactivate(Service service);

    void remove(Service service);

}
