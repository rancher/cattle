package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;

import java.util.Collection;

import javax.inject.Inject;

public class HostServiceDeploymentUnitLookup implements DeploymentUnitLookup {
    @Inject
    ServiceDao svcDao;

    @Override
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj, boolean transitioningOnly) {
        if (!(obj instanceof Host)) {
            return null;
        }
        Host host = (Host) obj;
        return svcDao.getServiceDeploymentUnitsOnHost(host, false);
    }
}
