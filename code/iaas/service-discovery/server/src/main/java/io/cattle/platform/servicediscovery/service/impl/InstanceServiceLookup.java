package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.Collection;

import javax.inject.Inject;

public class InstanceServiceLookup implements ServiceLookup {

    @Inject
    InstanceDao instanceDao;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance instance = (Instance)obj;
        return instanceDao.findServicesNonRemovedLinksOnly(instance);
    }

}
