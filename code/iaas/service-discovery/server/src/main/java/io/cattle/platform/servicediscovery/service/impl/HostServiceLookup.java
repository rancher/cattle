package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.Collection;

import javax.inject.Inject;

public class HostServiceLookup implements ServiceLookup {

    @Inject
    ServiceDao svcDao;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (!(obj instanceof Host)) {
            return null;
        }
        Host host = (Host) obj;
        return svcDao.getServicesOnHost(host.getId());
    }

}