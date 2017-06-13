package io.cattle.platform.servicediscovery.service.lookups;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class DeploymentUnitServiceLookup implements ServiceLookup {

    @Inject
    ObjectManager objectMgr;

    @Override
    public Collection<Long> getServices(Object obj) {
        if (!(obj instanceof DeploymentUnit)) {
            return null;
        }
        DeploymentUnit du = (DeploymentUnit)obj;
        return Arrays.asList(du.getServiceId());
    }

}
