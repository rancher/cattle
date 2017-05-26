package io.cattle.platform.servicediscovery.deployment.lookups;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Collection;

import javax.inject.Inject;

public class HostServiceDeploymentUnitLookup implements DeploymentUnitLookup {
    @Inject
    ObjectManager objectManager;

    @Override
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj) {
        if (!(obj instanceof Host)) {
            return null;
        }
        Host host = (Host) obj;
        return objectManager.find(DeploymentUnit.class,
                ObjectMetaDataManager.ACCOUNT_FIELD, host.getAccountId(),
                ObjectMetaDataManager.REMOVED_FIELD, null);
    }
}
