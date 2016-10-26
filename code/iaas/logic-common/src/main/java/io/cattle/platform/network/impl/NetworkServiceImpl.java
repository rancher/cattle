package io.cattle.platform.network.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;

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
    public String assignIpAddress(Network network, Object owner, String requestedIp) {
        if (network == null) {
            return null;
        }
        for (Subnet subnet : networkDao.getSubnets(network)) {
            PooledResourceOptions options = new PooledResourceOptions();
            if (requestedIp != null) {
                options.setRequestedItem(requestedIp);
            }
            PooledResource resource = poolManager.allocateOneResource(subnet, owner, options);
            if (resource != null) {
                return resource.getName();
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
    public String getNetworkMode(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        String mode = ObjectUtils.toString(labels.get(SystemLabels.LABEL_CNI_NETWORK));

        if (mode == null && "true".equals(labels.get(SystemLabels.LABEL_RANCHER_NETWORK))) {
            mode = NetworkConstants.NETWORK_MODE_MANAGED;
        }

        if (mode == null) {
            Map<String, Object> fields = DataUtils.getFields(instance);
            if (fields.containsKey(DockerInstanceConstants.FIELD_NETWORK_MODE)) {
                mode = DataAccessor.fieldString(instance, DockerInstanceConstants.FIELD_NETWORK_MODE);
            } else {
                mode = NetworkConstants.NETWORK_MODE_MANAGED;
            }
        }

        return mode;
    }

}