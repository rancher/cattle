package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.model.DeploymentUnit;

import java.util.Collection;

public interface DeploymentUnitLookup {
    Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj);
}
