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
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.Map;

import javax.inject.Inject;

public class PortUpdatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    InstanceDao instanceDao;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    HostDao hostDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { PortConstants.PROCESS_PORT_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port) state.getResource();

        IpAddress ip = objectManager.findOne(IpAddress.class, IP_ADDRESS.ID, port.getPublicIpAddressId(),
                IP_ADDRESS.REMOVED, null);
        if (ip == null) {
            return null;
        }


        Map<String, Object> data = state.getData();
        Integer oldPublicPort = null;
        if (data.get("old") != null) {
            Map<String, Object> old = CollectionUtils.toMap(data.get("old"));
            if (old.containsKey(PortConstants.FIELD_PUBLIC_POST)) {
                oldPublicPort = (Integer) old.get(PortConstants.FIELD_PUBLIC_POST);
            }
        }

        PublicEndpoint newEndPoint = new PublicEndpoint(ip.getAddress(), port.getPublicPort());

        PublicEndpoint oldEndPoint = new PublicEndpoint(ip.getAddress(), oldPublicPort);
        
        updateServiceEndpoints(port, newEndPoint, oldEndPoint);

        updateHostEndPoints(ip, newEndPoint, oldEndPoint);

        return null;
    }

    protected void updateHostEndPoints(IpAddress ip, PublicEndpoint newEndPoint, PublicEndpoint oldEndPoint) {
        Host host = hostDao.getHostForIpAddress(ip.getId());
        if (host != null) {
            sdService.updateHostPublicEndpoints(host, newEndPoint, true);
            sdService.updateHostPublicEndpoints(host, oldEndPoint, false);
        }
    }

    protected void updateServiceEndpoints(Port port, PublicEndpoint newEndPoint, PublicEndpoint oldEndPoint) {
        Service service = instanceDao.getServiceManaging(port.getInstanceId());
        if (service != null) {
            sdService.updateServicePublicEndpoints(service, newEndPoint, true);
            sdService.updateServicePublicEndpoints(service, oldEndPoint, false);
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

