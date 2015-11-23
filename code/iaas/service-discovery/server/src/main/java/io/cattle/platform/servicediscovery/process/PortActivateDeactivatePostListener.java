package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
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
public class PortActivateDeactivatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    HostDao hostDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { PortConstants.PROCESS_PORT_ACTIVATE, PortConstants.PROCESS_PORT_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port) state.getResource();

        IpAddress ip = objectManager.findOne(IpAddress.class, IP_ADDRESS.ID, port.getPublicIpAddressId(),
                IP_ADDRESS.REMOVED, null);
        if (ip == null) {
            return null;
        }

        boolean add = process.getName().equalsIgnoreCase(PortConstants.PROCESS_PORT_ACTIVATE);

        PublicEndpoint endPoint = new PublicEndpoint(ip.getAddress(), port.getPublicPort());

        updateServiceEndpoints(port, add, endPoint);

        updateHostEndPoints(ip, add, endPoint);

        return null;
    }

    protected void updateHostEndPoints(IpAddress ip, boolean add, PublicEndpoint endPoint) {
        Host host = hostDao.getHostForIpAddress(ip.getId());
        if (host != null) {
            sdService.updateHostPublicEndpoints(host, endPoint, add);
        }
    }

    protected void updateServiceEndpoints(Port port, boolean add, PublicEndpoint endPoint) {
        Service service = instanceDao.getServiceManaging(port.getInstanceId());
        if (service != null) {
            sdService.updateServicePublicEndpoints(service, endPoint, add);
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
