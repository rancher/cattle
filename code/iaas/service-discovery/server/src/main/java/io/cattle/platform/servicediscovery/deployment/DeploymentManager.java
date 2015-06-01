package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

public interface DeploymentManager extends AnnotatedEventListener {

    void activate(Service service);

    void deactivate(Service service);

    void remove(Service service);

    void activateGlobalServicesForHost(long accountId, long hostId);

    void reconcileServicesFor(Object obj);

    @EventHandler
    void serviceUpdate(ConfigUpdate update);

}
