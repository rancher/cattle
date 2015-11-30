package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.iaas.healthcheck.service.HealthcheckInstancesLookup;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.List;

import javax.inject.Inject;

public class AgentHealthcheckInstancesLookup extends AbstractJooqDao implements HealthcheckInstancesLookup {

    @Inject
    ServiceDao serviceDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Instance> getInstances(Object obj) {
        if (!(obj instanceof Agent)) {
            return null;
        }
        Agent agent = (Agent) obj;
        Host host = objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
        if (host == null) {
            return null;
        }
        return serviceDao.getInstancesWithHealtcheckEnabled(host.getAccountId());
    }
}
