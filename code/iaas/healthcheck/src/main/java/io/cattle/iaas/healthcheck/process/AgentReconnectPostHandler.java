package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.iaas.healthcheck.service.HealthcheckService.HealthcheckInstanceType;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AgentReconnectPostHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ServiceDao serviceDao;
    @Inject
    HealthcheckService healthcheckService;

    @Override
    public String[] getProcessNames() {
        return new String[] { AgentConstants.PROCESS_RECONNECT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Host host = null;

        Agent agent = (Agent) state.getResource();
        host = objectManager.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);

        if (host == null) {
            return null;
        }
        List<? extends Instance> instances = serviceDao.getInstancesWithHealtcheckRunningOnHost(host.getId());

        reregisterInstancesForHealtchecks(instances);

        return null;
    }

    protected void reregisterInstancesForHealtchecks(List<? extends Instance> instances) {
        for (Instance instance : instances) {
            healthcheckService.registerForHealtcheck(HealthcheckInstanceType.INSTANCE, instance.getId());
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}