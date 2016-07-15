package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PortActivateDeactivatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    HostDao hostDao;

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { PortConstants.PROCESS_PORT_ACTIVATE, PortConstants.PROCESS_PORT_UPDATE,
                PortConstants.PROCESS_PORT_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port) state.getResource();

        List<? extends Host> hosts = instanceDao.findHosts(port.getAccountId(), port.getInstanceId());
        Instance instance = objectManager.loadResource(Instance.class, port.getInstanceId());
        List<? extends Service> services = instanceDao.findServicesFor(instance);

        for (Host host : hosts) {
            sdService.reconcileHostEndpoints(host);
        }

        for (Service service : services) {
            sdService.reconcileServiceEndpoints(service);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
