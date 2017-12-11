package io.cattle.platform.network.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class NetworkServiceImpl implements NetworkService {

    public static final Map<String, String> MODE_TO_KIND = new HashMap<>();

    static {
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_HOST, NetworkConstants.KIND_DOCKER_HOST);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_NONE, NetworkConstants.KIND_DOCKER_NONE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_BRIDGE, NetworkConstants.KIND_DOCKER_BRIDGE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_DEFAULT, NetworkConstants.KIND_DOCKER_BRIDGE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_CONTAINER, NetworkConstants.KIND_DOCKER_CONTAINER);
    }

    @Inject
    NetworkDao networkDao;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ResourcePoolManager poolManager;

    @Override
    public Network resolveNetwork(long accountId, String networkName) {
        if (networkName == null) {
            return null;
        }

        String mode = networkName;
        String kind = MODE_TO_KIND.get(mode);

        if (kind != null) {
            return networkDao.getNetworkByKind(accountId, kind);
        }

        if (NetworkConstants.NETWORK_MODE_MANAGED.equals(networkName)) {
            return networkDao.getDefaultNetwork(accountId);
        }

        return networkDao.getNetworkByName(accountId, networkName);
    }

    @Override
    public boolean shouldAssignIpAddress(Network network) {
        if (network == null) {
            return false;
        }
        List<Subnet> subnets = networkDao.getSubnets(network);
        return subnets.size() > 0;
    }

    @Override
    public IPAssignment assignIpAddress(Network network, Object owner, List<String> requestedIps) {
        if (network == null) {
            return null;
        }
        for (Subnet subnet : networkDao.getSubnets(network)) {
            PooledResourceOptions options = new PooledResourceOptions();
            if (requestedIps != null) {
                options.setRequestedItems(requestedIps);
            }
            PooledResource resource = poolManager.allocateOneResource(subnet, owner, options);
            if (resource != null) {
                return new IPAssignment(resource.getName(), subnet);
            }
        }
        return null;
    }

    @Override
    public void releaseIpAddress(Network network, Object owner) {
        if (network == null) {
            return;
        }

        for (Subnet subnet : networkDao.getSubnets(network)) {
            poolManager.releaseResource(subnet, owner);
        }
    }

    @Override
    public String getNetworkMode(Map<String, Object> instanceData) {
        Map<String, Object> labels = CollectionUtils.toMap(instanceData.get(InstanceConstants.FIELD_LABELS));
        String mode = ObjectUtils.toString(labels.get(SystemLabels.LABEL_CNI_NETWORK));
        
        if(mode == null) {
            String dataMode = ObjectUtils.toString(instanceData.get(DockerInstanceConstants.FIELD_NETWORK_MODE));
            if (inAllowedModesForManaged(dataMode) && "true".equals(labels.get(SystemLabels.LABEL_RANCHER_NETWORK))) {
                mode = NetworkConstants.NETWORK_MODE_MANAGED;
            } else {
                mode = dataMode;
            }
        }

        return mode;
    }
    
    private boolean inAllowedModesForManaged(String dataMode) {
        return dataMode==null || dataMode.equals(NetworkConstants.NETWORK_MODE_NONE) 
        || dataMode.equals(NetworkConstants.NETWORK_MODE_DEFAULT) || dataMode.equals(NetworkConstants.NETWORK_MODE_BRIDGE);
    }
}
