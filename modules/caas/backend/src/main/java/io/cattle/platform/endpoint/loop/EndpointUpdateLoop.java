package io.cattle.platform.endpoint.loop;

import static java.util.stream.Collectors.*;

import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class EndpointUpdateLoop implements Loop {

    long accountId;
    EnvironmentResourceManager envResourceManager;
    ObjectManager objectManager;

    public EndpointUpdateLoop(long accountId, EnvironmentResourceManager envResourceManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.envResourceManager = envResourceManager;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(Object input) {
        Metadata metadata = envResourceManager.getMetadata(accountId);
        Map<Long, String> agentIps = new HashMap<>();
        Map<Long, ServiceInfo> services = metadata.getServices().stream().collect(toMap((x) -> x.getId(), (x) -> x));
        Map<Long, Set<PortInstance>> servicePorts = new HashMap<>();
        Map<Long, Set<PortInstance>> hostPorts = new HashMap<>();

        for (HostInfo hostInfo : metadata.getHosts()) {
            if (StringUtils.isNotBlank(hostInfo.getAgentIp())) {
                agentIps.put(hostInfo.getId(), hostInfo.getAgentIp());
            }
        }

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            ServiceInfo serviceInfo = services.get(instanceInfo.getServiceId());
            Set<PortInstance> ports = new HashSet<>();

            for (PortInstance port : instanceInfo.getPorts()) {
                port.setAgentIpAddress(agentIps.get(instanceInfo.getHostId()));
                port.setFqdn(serviceInfo == null ? null : serviceInfo.getFqdn());
                port.setHostId(instanceInfo.getHostId());
                port.setInstanceId(instanceInfo.getId());
                port.setServiceId(instanceInfo.getServiceId());

                ports.add(port);

                if (serviceInfo != null) {
                    add(servicePorts, serviceInfo.getId(), port);
                }
                add(hostPorts, instanceInfo.getHostId(), port);
            }

            if (!instanceInfo.getPorts().equals(ports)) {
                metadata.modify(Instance.class, instanceInfo.getId(), (instance) -> {
                    return objectManager.setFields(instance, InstanceConstants.FIELD_PORT_BINDINGS, ports);
                });
            }
        }

        for (ServiceInfo serviceInfo : metadata.getServices()) {
            Set<PortInstance> ports = servicePorts.get(serviceInfo.getId());
            if (ports == null && serviceInfo.getPorts().size() == 0) {
                continue;
            }

            if (!serviceInfo.getPorts().equals(ports)) {
                metadata.modify(Service.class, serviceInfo.getId(), (service) -> {
                    return objectManager.setFields(service, ServiceConstants.FIELD_PUBLIC_ENDPOINTS, ports);
                });
            }
        }

        for (HostInfo hostInfo : metadata.getHosts()) {
            Set<PortInstance> ports = hostPorts.get(hostInfo.getId());
            if (ports == null && hostInfo.getPorts().size() == 0) {
                continue;
            }

            if (!hostInfo.getPorts().equals(ports)) {
                metadata.modify(Host.class, hostInfo.getId(), (host) -> {
                    return objectManager.setFields(host, HostConstants.FIELD_PUBLIC_ENDPOINTS, ports);
                });
            }
        }

        return Result.DONE;
    }


    private void add(Map<Long, Set<PortInstance>> map, Long id, PortInstance port) {
        if (id == null) {
            return;
        }

        Set<PortInstance> ports = map.get(id);
        if (ports == null) {
            ports = new HashSet<>();
            map.put(id, ports);
        }

        ports.add(port);
    }

}
