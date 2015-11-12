package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRemovePreHandler extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    GenericMapDao mapDao;

    @Inject
    HealthcheckService healthcheckService;

    @Inject
    ServiceDao serviceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { HostConstants.PROCESS_REMOVE, AgentConstants.PROCESS_RECONNECT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Host host = null;
        if (process.getName().equals(HostConstants.PROCESS_REMOVE)) {
            host = (Host) state.getResource();
        } else {
            Agent agent = (Agent) state.getResource();
            host = objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
        }

        if (host == null) {
            return null;
        }

        removeHealthCheckHostMaps(host);

        return null;
    }

    protected void removeHealthCheckHostMaps(Host host) {
        List<? extends HealthcheckInstanceHostMap> healthHostMaps = mapDao.findNonRemoved(
                HealthcheckInstanceHostMap.class,
                Host.class, host.getId());
        for (HealthcheckInstanceHostMap healthHostMap : healthHostMaps) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, healthHostMap, null);
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
