package io.cattle.platform.loop;

import io.cattle.platform.core.addon.PortInstance;
import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HostEndpointUpdateLoop implements Loop {
    private static Set<String> validStates = CollectionUtils.set(InstanceConstants.STATE_RUNNING, InstanceConstants.STATE_STARTING,
            InstanceConstants.STATE_RESTARTING);
    long accountId;
    MetadataManager metadataManager;
    ObjectManager objectManager;

    public HostEndpointUpdateLoop(long accountId, MetadataManager metadataManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.metadataManager = metadataManager;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(List<Object> input) {
        Metadata metadata = metadataManager.getMetadataForAccount(accountId);
        Map<Long, Set<PortInstance>> hostPorts = new HashMap<>();

        for (EnvironmentInfo env : metadata.getEnvironments()) {
            Metadata envMetadata = metadataManager.getMetadataForAccount(env.getAccountId());
            for (InstanceInfo instanceInfo : envMetadata.getInstances()) {
                if (!validStates.contains(instanceInfo.getState())) {
                    continue;
                }
                for (PortInstance port : instanceInfo.getPorts()) {
                    add(hostPorts, instanceInfo.getHostId(), port);
                }
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
