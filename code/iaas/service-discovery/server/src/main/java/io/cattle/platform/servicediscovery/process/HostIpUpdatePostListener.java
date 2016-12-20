package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostIpUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    HostDao hostDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { IpAddressConstants.PROCESS_IP_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ip = (IpAddress) state.getResource();

        Host host = hostDao.getHostForIpAddress(ip.getId());

        if (host == null) {
            return null;
        }

        sdService.reconcileHostEndpoints(host);

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
