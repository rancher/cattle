package io.cattle.platform.inator;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.eventing.annotation.EventHandler;

import java.util.Collection;

public interface Deployinator {

    Result reconcile(Class<?> clz, Long id);

    @EventHandler
    void serviceUpdate(ConfigUpdate update);

    @EventHandler
    void deploymentUnitUpdate(ConfigUpdate update);

    void reconcileServices(Collection<? extends Service> services);

    void scheduleReconcile(DeploymentUnit unit);

}
