package io.cattle.platform.servicediscovery.deployment.lookups;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;

import javax.inject.Inject;

public class InstanceDeploymentUnitLookup implements DeploymentUnitLookup {
    @Inject
    ObjectManager objMgr;

    @Override
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance instance = (Instance) obj;
        return objMgr.find(DeploymentUnit.class,
                DEPLOYMENT_UNIT.ID, instance.getDeploymentUnitId());
    }
}
