package io.cattle.platform.servicediscovery.lookups;

import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;

import javax.inject.Inject;

public class SkipServiceLookup implements ServiceLookup {

    @Inject
    HostDao hostDao;
    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objMgr;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        Long accountId = null;
        if (obj instanceof Host) {
            accountId = ((Host) obj).getAccountId();
        } else if (obj instanceof Agent) {
            Agent agent = (Agent) obj;
            Host host = objMgr.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
            if (host == null) {
                return null;
            }
            accountId = host.getAccountId();
        }

        if (accountId == null) {
            return null;
        }

        if (hostDao.hasActiveHosts(accountId)) {
            return serviceDao.getSkipServices(accountId);
        }

        return null;
    }

}