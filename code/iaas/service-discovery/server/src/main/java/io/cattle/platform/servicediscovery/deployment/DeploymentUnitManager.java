package io.cattle.platform.servicediscovery.deployment;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

public interface DeploymentUnitManager extends AnnotatedEventListener {

    void deactivate(DeploymentUnit unit);

    void remove(DeploymentUnit unit, String reason, String level);

    void scheduleReconcile(DeploymentUnit unit);

    @EventHandler
    void deploymentUnitUpdate(ConfigUpdate update);
    
    void activate(DeploymentUnit unit);

    boolean isUnhealthy(DeploymentUnit unit);

    boolean isGlobal(DeploymentUnit unit);
}
