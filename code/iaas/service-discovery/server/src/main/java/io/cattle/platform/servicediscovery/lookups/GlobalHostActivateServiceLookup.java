package io.cattle.platform.servicediscovery.lookups;

import static io.cattle.platform.core.constants.ExternalEventConstants.*;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostLabelMap;
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
    public Collection<? extends Service> getServices(Object obj) {
        if (obj == null) {
            return null;
        }

        Long accountId = null;
        if (obj instanceof Host) {
            Host host = (Host) obj;
            accountId = host.getAccountId();
        } else if (obj instanceof HostLabelMap) {
            HostLabelMap m = (HostLabelMap) obj;
            accountId = m.getAccountId();
        } else if (obj instanceof ExternalEvent) {
            ExternalEvent event = (ExternalEvent)obj;
            if (KIND_EXTERNAL_HOST_EVENT.equals(event.getKind()) && TYPE_SCHEDULER_UPDATE.equals(event.getEventType())) {
                accountId = event.getAccountId();
            }
        }

        if (accountId == null) {
            return null;
        }

        List<? extends Service> services = expMapDao.getActiveServices(accountId);
        List<Service> activeGlobalServices = new ArrayList<Service>();
        for (Service service : services) {
            if (ServiceUtil.isGlobalService(service)) {
                activeGlobalServices.add(service);
            }
        }
        return activeGlobalServices;
    }

}