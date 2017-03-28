package io.cattle.platform.servicediscovery.deployment.lookups;

import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.DeploymentUnit;
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
    public Collection<? extends DeploymentUnit> getDeploymentUnits(Object obj, boolean transitioningOnly) {
        if (!(obj instanceof Agent)) {
            return null;
        }
        Agent agent = (Agent) obj;
        Host host = objMgr.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
        if (host == null) {
            return null;
        }
        return svcDao.getServiceDeploymentUnitsOnHost(host, transitioningOnly);
    }
}
