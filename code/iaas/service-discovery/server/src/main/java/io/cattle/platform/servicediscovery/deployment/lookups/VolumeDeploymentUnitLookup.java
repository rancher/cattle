package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class VolumeDeploymentUnitLookup implements DeploymentUnitLookup {
    @Inject
    ObjectManager objMgr;
    @Inject
    ServiceDao svcDao;

    @Override
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj) {
        if (!(obj instanceof Volume)) {
            return null;
        }
        DeploymentUnit unit = objMgr.loadResource(DeploymentUnit.class, ((Volume) obj).getDeploymentUnitId());
        return unit == null ? null : Arrays.asList(unit);
    }
}
