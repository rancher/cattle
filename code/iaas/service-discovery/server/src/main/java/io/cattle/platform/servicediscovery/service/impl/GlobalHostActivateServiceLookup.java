package io.cattle.platform.servicediscovery.service.impl;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostLabelMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.service.ServiceLookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class GlobalHostActivateServiceLookup implements ServiceLookup {

    @Inject
    ServiceDao svcDao;

    @Inject
    ServiceExposeMapDao expMapDao;

    @Inject
    ServiceDiscoveryService sdSvc;

    @Inject
    AllocatorService allocatorSvc;

    @Inject
    ObjectManager objMgr;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        Host host = null;
        if (obj instanceof Host) {
            host = (Host) obj;
        } else if (obj instanceof HostLabelMap) {
            host = objMgr.loadResource(Host.class, ((HostLabelMap) obj).getHostId());
        } else {
            return null;
        }

        List<? extends Service> services = expMapDao.getActiveServices(host.getAccountId());
        List<Service> activeGlobalServices = new ArrayList<Service>();
        for (Service service : services) {
            Map<String, String> serviceLabels = ServiceDiscoveryUtil.getMergedServiceLabels(service, allocatorSvc);
            if ((sdSvc.isGlobalService(service) || sdSvc.isScalePolicyService(service)) &&
                    allocatorSvc.hostChangesAffectsHostAffinityRules(host.getId(), serviceLabels)) {
                activeGlobalServices.add(service);
            }
        }
        return activeGlobalServices;
    }

}