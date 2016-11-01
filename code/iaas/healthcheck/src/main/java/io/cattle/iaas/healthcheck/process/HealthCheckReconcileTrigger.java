package io.cattle.iaas.healthcheck.process;

import io.cattle.iaas.healthcheck.service.HealthcheckHostLookup;
import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.GenericMapDao;
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
public class HealthCheckReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    GenericMapDao mapDao;

    @Inject
    HealthcheckService healthcheckService;

    @Inject
    ServiceDao serviceDao;

    @Inject
    List<HealthcheckHostLookup> hostLookups;

    @Override
    public String[] getProcessNames() {
        return new String[] { HostConstants.PROCESS_REMOVE, AgentConstants.PROCESS_RECONNECT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Host host = null;
        for (HealthcheckHostLookup lookup : hostLookups) {
            host = lookup.getHost(state.getResource());
            if (host != null) {
                break;
            }
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
