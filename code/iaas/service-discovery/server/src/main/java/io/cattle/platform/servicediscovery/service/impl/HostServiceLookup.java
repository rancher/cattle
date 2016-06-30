package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class HostServiceLookup implements ServiceLookup {

    @Inject
    ServiceDao svcDao;

    @Inject
    ObjectManager objMgr;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (!(obj instanceof Host)) {
            return null;
        }
        Host host = (Host) obj;
        List<Service> services = new ArrayList<>();
        // add all services on host
        services.addAll(svcDao.getServicesOnHost(host.getId()));

        // add all services with scale policy
        List<? extends Service> allServices = objMgr.find(Service.class, SERVICE.ACCOUNT_ID, host.getAccountId(),
                SERVICE.REMOVED, null);
        for (Service service : allServices) {
            if (sdService.isScalePolicyService(service)) {
                services.add(service);
            }
        }

        return services;
    }

}