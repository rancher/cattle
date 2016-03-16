package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
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

import java.util.List;
import java.util.Map;

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
        return new String[] { PortConstants.PROCESS_PORT_ACTIVATE, PortConstants.PROCESS_PORT_UPDATE,
                PortConstants.PROCESS_PORT_DEACTIVATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Port port = (Port) state.getResource();

        IpAddress ip = objectManager.findOne(IpAddress.class, IP_ADDRESS.ID, port.getPublicIpAddressId(),
                IP_ADDRESS.REMOVED, null);
        if (ip == null) {
            return null;
        }
        boolean add = process.getName().equalsIgnoreCase(PortConstants.PROCESS_PORT_ACTIVATE)
                || process.getName().equalsIgnoreCase(PortConstants.PROCESS_PORT_UPDATE);

        Map<String, Object> data = state.getData();
        Integer oldPublicPort = null;
        if (data.get("old") != null) {
            Map<String, Object> old = CollectionUtils.toMap(data.get("old"));
            if (old.containsKey(PortConstants.FIELD_PUBLIC_POST)) {
                oldPublicPort = (Integer) old.get(PortConstants.FIELD_PUBLIC_POST);
            }
        }

        Host host = hostDao.getHostForIpAddress(ip.getId());
        Instance instance = objectManager.loadResource(Instance.class, port.getInstanceId());
        List<? extends Service> services = instanceDao.findServicesFor(objectManager.loadResource(Instance.class,
                port.getInstanceId()));
        if (services.size() == 0) {
            // standalone instance
            services.add(null);
        }
        for (Service service : services) {
            PublicEndpoint newEndPoint = new PublicEndpoint(ip.getAddress(), port.getPublicPort(), host, instance, service);
            sdService.propagatePublicEndpoint(newEndPoint, add);
            if (add) {
                PublicEndpoint oldEndPoint = new PublicEndpoint(ip.getAddress(), oldPublicPort, host, instance, service);
                sdService.propagatePublicEndpoint(oldEndPoint, false);
            }
        }

        return null;
    }


    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
