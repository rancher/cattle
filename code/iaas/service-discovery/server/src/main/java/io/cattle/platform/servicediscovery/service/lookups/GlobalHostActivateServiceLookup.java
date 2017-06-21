package io.cattle.platform.servicediscovery.service.lookups;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class GlobalHostActivateServiceLookup implements ServiceLookup {
    @Inject
    ServiceExposeMapDao expMapDao;

    @Inject
    ServiceDiscoveryService sdSvc;

    @Inject
    AllocationHelper allocationHelper;

    @Inject
    ObjectManager objMgr;

    @Override
    public Collection<Long> getServices(Object obj) {
        if (obj == null) {
            return null;
        }

        Long accountId = null;
        if (obj instanceof Host) {
            Host host = (Host) obj;
            accountId = host.getAccountId();
        }

        if (accountId == null) {
            return null;
        }

        List<? extends Service> allServices = objMgr.find(Service.class,
                SERVICE.ACCOUNT_ID, accountId,
                SERVICE.REMOVED, null);

        List<Long> activeGlobalServices = new ArrayList<>();
        for (Service service : allServices) {
            if (ServiceUtil.isGlobalService(service)) {
                activeGlobalServices.add(service.getId());
            }
        }
        return activeGlobalServices;
    }

}