package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.PortTable.PORT;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
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
        
        Map<String, Object> data = state.getData();
        String oldAddress = "";
        if (data.get("old") != null) {
            Map<String, Object> old = CollectionUtils.toMap(data.get("old"));
            if (old.containsKey(IpAddressConstants.FIELD_ADDRESS)) {
                oldAddress = old.get(IpAddressConstants.FIELD_ADDRESS).toString();
            }
        }
        String newAddress = ip.getAddress();
        if (oldAddress.equalsIgnoreCase(newAddress)) {
            return null;
        }
        // find all instances deployed on the host with the ports opened
        List<? extends Instance> instances = instanceDao.getNonRemovedInstanceOn(host.getId());
        for (Instance instance : instances) {
            for (Port port : objectManager.find(Port.class, PORT.INSTANCE_ID, instance.getId(), PORT.REMOVED, null,
                    PORT.STATE, CommonStatesConstants.ACTIVE)) {
                List<? extends Service> services = instanceDao.findServicesFor(instance);
                for (Service service : services) {
                    // 1.remove old end point
                    PublicEndpoint oldEndpoint = new PublicEndpoint(oldAddress, port.getPublicPort(), host, instance,
                            service);
                    sdService.propagatePublicEndpoint(oldEndpoint, false);

                    // 2.create new end point
                    PublicEndpoint newEndpoint = new PublicEndpoint(newAddress, port.getPublicPort(), host, instance,
                            service);
                    sdService.propagatePublicEndpoint(newEndpoint, true);
                }
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
