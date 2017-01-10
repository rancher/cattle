package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

import java.util.Collection;

public interface DeploymentManager extends AnnotatedEventListener {

    void activate(Service service);

    void deactivate(Service service);

    void remove(Service service);

    @EventHandler
    void serviceUpdate(ConfigUpdate update);

    void reconcileServices(Collection<? extends Service> services);

    boolean isHealthy(Service service);
}
