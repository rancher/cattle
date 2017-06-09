package io.cattle.platform.loop.trigger;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostServiceEndpointTrigger implements Trigger {

    @Inject
    InstanceDao instanceDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    HostDao hostDao;
    @Inject
    GenericMapDao mapDao;
    @Inject
    LoopManager loopManager;

    @Override
    public void trigger(ProcessInstance process) {
        Object resource = process.getResource();
        if (resource instanceof Port) {
            handlePort((Port) resource);
        } else if (resource instanceof IpAddress &&
                IpAddressConstants.PROCESS_IP_UPDATE.equals(process.getName())) {
            handleIpAddress((IpAddress) resource);
        }
    }

    private void handlePort(Port port) {
        List<? extends Host> hosts = instanceDao.findHosts(port.getAccountId(), port.getInstanceId());
        Instance instance = objectManager.loadResource(Instance.class, port.getInstanceId());
        List<? extends Service> services = instanceDao.findServicesFor(instance);

        for (Host host : hosts) {
            loopManager.kick(LoopFactoryImpl.HOST_ENDPOINTS, HostConstants.TYPE, host.getId(), null);
        }

        for (Service service : services) {
            loopManager.kick(LoopFactoryImpl.SERVICE_ENDPOINTS, ServiceConstants.KIND_SERVICE, service.getId(), null);
        }
    }

    private void handleIpAddress(IpAddress ip) {
        Host host = hostDao.getHostForIpAddress(ip.getId());

        if (host == null) {
            return;
        }

        loopManager.kick(LoopFactoryImpl.HOST_ENDPOINTS, HostConstants.TYPE, host.getId(), null);
    }

}