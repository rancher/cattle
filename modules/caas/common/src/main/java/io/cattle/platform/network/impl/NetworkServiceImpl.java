package io.cattle.platform.network.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkServiceImpl implements NetworkService {

    public static final Map<String, String> MODE_TO_KIND = new HashMap<>();

    static {
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_HOST, NetworkConstants.KIND_DOCKER_HOST);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_NONE, NetworkConstants.KIND_DOCKER_NONE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_BRIDGE, NetworkConstants.KIND_DOCKER_BRIDGE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_DEFAULT, NetworkConstants.KIND_DOCKER_BRIDGE);
        MODE_TO_KIND.put(NetworkConstants.NETWORK_MODE_CONTAINER, NetworkConstants.KIND_DOCKER_CONTAINER);
    }

    ObjectManager objectManager;
    NetworkDao networkDao;
    JsonMapper jsonMapper;
    ResourcePoolManager poolManager;

    public NetworkServiceImpl(NetworkDao networkDao, JsonMapper jsonMapper, ResourcePoolManager poolManager) {
        super();
        this.networkDao = networkDao;
        this.jsonMapper = jsonMapper;
        this.poolManager = poolManager;
    }

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
    public IPAssignment assignIpAddress(Network network, Object owner, String requestedIp) {
        if (network == null) {
            return null;
        }
        for (Subnet subnet : networkDao.getSubnets(network)) {
            PooledResourceOptions options = new PooledResourceOptions();
            if (requestedIp != null) {
                options.setRequestedItem(requestedIp);
            }
            PooledResource resource = assignIpAddressFromSubnet(subnet, owner, options);
            if (resource != null) {
                return new IPAssignment(resource.getName(), subnet);
            }
        }
        return null;
    }

    private PooledResource assignIpAddressFromSubnet(Subnet subnet, Object owner, PooledResourceOptions options) {
        if (owner instanceof Instance && ((Instance) owner).getDeploymentUnitId() != null && DataAccessor.fieldBool(owner, InstanceConstants.FIELD_RETAIN_IP)) {
            DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, ((Instance) owner).getDeploymentUnitId());
            options.setSubOwner(DataAccessor.fieldString(owner, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME));
            return poolManager.allocateOneResource(subnet, unit, options);
        }

        return poolManager.allocateOneResource(subnet, owner, options);
    }

    @Override
    public void releaseIpAddress(Network network, Object owner) {
        if (network == null) {
            return;
        }

        if (owner instanceof Instance && ((Instance) owner).getDeploymentUnitId() != null && DataAccessor.fieldBool(owner, InstanceConstants.FIELD_RETAIN_IP)) {
            // We don't release IPs for these, the DU release them
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

        if (mode == null && "true".equals(labels.get(SystemLabels.LABEL_RANCHER_NETWORK))) {
            mode = NetworkConstants.NETWORK_MODE_MANAGED;
        }

        if (mode == null) {
            if (instanceData.containsKey(InstanceConstants.FIELD_NETWORK_MODE)) {
                mode = ObjectUtils.toString(instanceData.get(InstanceConstants.FIELD_NETWORK_MODE));
            } else {
                mode = NetworkConstants.NETWORK_MODE_MANAGED;
            }
        }

        return mode;
    }

}