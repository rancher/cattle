package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.core.model.Service;

public interface DeploymentManager {

    void activate(Service service);

    void deactivate(Service service);

    void remove(Service service);

    void activateGlobalServicesForHost(long accountId, long hostId);

}
