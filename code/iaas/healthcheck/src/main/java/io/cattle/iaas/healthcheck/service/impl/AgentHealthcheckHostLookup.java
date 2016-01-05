package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.iaas.healthcheck.service.HealthcheckHostLookup;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

public class AgentHealthcheckHostLookup extends AbstractJooqDao implements HealthcheckHostLookup {

    @Inject
    ObjectManager objectManager;

    @Override
    public Host getHost(Object obj) {
        if (!(obj instanceof Agent)) {
            return null;
        }
        Agent agent = (Agent) obj;
        return objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
    }

}
