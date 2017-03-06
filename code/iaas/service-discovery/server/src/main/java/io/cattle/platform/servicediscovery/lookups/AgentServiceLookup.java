package io.cattle.platform.servicediscovery.lookups;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class AgentServiceLookup extends AbstractJooqDao implements ServiceLookup {
    @Inject
    ObjectManager objMgr;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (!(obj instanceof Agent)) {
            return null;
        }
        Agent agent = (Agent) obj;
        Host host = objMgr.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
        if (host == null) {
            return null;
        }
        List<? extends Service> allServices = objMgr.find(Service.class, SERVICE.ACCOUNT_ID, host.getAccountId(),
                SERVICE.REMOVED, null);
        List<Service> svcsToReconcile = new ArrayList<>();
        for (Service service : allServices) {
            if (!ServiceUtil.isActiveService(service)) {
                continue;
            }
            if (ServiceUtil.isGlobalService(service)) {
                svcsToReconcile.add(service);
            }
        }
        return svcsToReconcile;
    }

}
