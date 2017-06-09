package io.cattle.platform.servicediscovery.deployment.lookups;

import static io.cattle.platform.core.model.tables.HostTable.*;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;

import java.util.Collection;

import javax.inject.Inject;

public class AgentServiceDeploymentUnitLookup implements DeploymentUnitLookup {
    @Inject
    ObjectManager objMgr;
    @Inject
    ServiceDao svcDao;

    @Override
    public Collection<Long> getDeploymentUnits(Object obj) {
        Host host = null;
        if (obj instanceof Host) {
            host = (Host) obj;
        }
        if (obj instanceof Agent) {
            Agent agent = (Agent) obj;
            host = objMgr.findAny(Host.class, HOST.AGENT_ID, agent.getId());
        }
        if (host == null) {
            return null;
        }
        return svcDao.getServiceDeploymentUnitsOnHost(host);
    }
}
