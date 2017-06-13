package io.cattle.platform.servicediscovery.deployment.lookups;

import java.util.Collection;

public interface DeploymentUnitLookup {

    Collection<Long> getDeploymentUnits(Object obj);

}
