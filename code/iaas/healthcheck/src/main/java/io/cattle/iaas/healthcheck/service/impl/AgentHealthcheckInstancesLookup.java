package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.iaas.healthcheck.service.HealthcheckInstancesLookup;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class AgentHealthcheckInstancesLookup extends AbstractJooqDao implements HealthcheckInstancesLookup {

    @Inject
    ServiceDao serviceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Instance> getInstances(Object obj) {
        Host host = null;

        if (obj instanceof Agent) {
            Agent agent = (Agent) obj;
            host = objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
        } else if (obj instanceof Host) {
            host = (Host) obj;
        }

        if (host == null) {
            return null;
        }
        return serviceDao.getInstancesWithHealtcheckEnabled(host.getAccountId());
    }
}
