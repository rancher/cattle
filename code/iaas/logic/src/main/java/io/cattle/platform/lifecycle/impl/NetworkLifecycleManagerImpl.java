package io.cattle.platform.lifecycle.impl;

import static io.cattle.platform.object.util.DataAccessor.*;

import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.cache.EnvironmentResourceManager;
import io.cattle.platform.core.constants.DockerInstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.NetworkLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

public class NetworkLifecycleManagerImpl implements NetworkLifecycleManager {

    ObjectManager objectManager;
    NetworkService networkService;
    ResourcePoolManager poolManager;
    ConfigItemStatusManager statusManager;
    EnvironmentResourceManager envResourceManager;

    public NetworkLifecycleManagerImpl(ObjectManager objectManager, NetworkService networkService, ResourcePoolManager poolManager,
            ConfigItemStatusManager statusManager, EnvironmentResourceManager envResourceManager) {
        super();
        this.objectManager = objectManager;
        this.networkService = networkService;
        this.poolManager = poolManager;
        this.statusManager = statusManager;
        this.envResourceManager = envResourceManager;
    }

    @Override
    public void create(Instance instance, Stack stack) throws LifecycleException {
        setupRequestedIp(instance);
        Network network = resolveNetworkMode(instance);
        setDns(instance, stack, network);
    }

    @Override
    public void preStart(Instance instance) {
        setPskForIpsec(instance);
    }

    @Override
    public void preRemove(Instance instance) {
        Network network = objectManager.loadResource(Network.class,
                fieldLong(instance, InstanceConstants.FIELD_PRIMARY_NETWORK_ID));

        releaseMacAddress(instance, network);
        releaseIpAddress(instance, network);
    }

    @Override
    public void assignNetworkResources(Instance instance) throws LifecycleException {
        Long networkId = fieldLong(instance, InstanceConstants.FIELD_PRIMARY_NETWORK_ID);
        Network network = objectManager.loadResource(Network.class, networkId);
        if (network == null) {
            return;
        }

        assignIpAddress(instance, network);
        assignMacAddress(instance, network);
        setupCNILabels(instance, network);
    }

    private void setupRequestedIp(Instance instance) {
        String ip = getLabel(instance, SystemLabels.LABEL_REQUESTED_IP);
        if (StringUtils.isNotBlank(ip)) {
            setLabel(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS, ip);
        }
    }

    private Network resolveNetworkMode(Instance instance) throws LifecycleException {
        String mode = networkService.getNetworkMode(DataUtils.getFields(instance));

        Network network = networkService.resolveNetwork(instance.getAccountId(), mode);
        if (network == null && StringUtils.isNotBlank(mode) && !instance.getNativeContainer()) {
            throw new LifecycleException(String.format("Failed to find network for networkMode %s", mode));
        }

        if (network != null) {
            setField(instance, InstanceConstants.FIELD_NETWORK_IDS, Arrays.asList(network.getId()));
            setField(instance, InstanceConstants.FIELD_PRIMARY_NETWORK_ID, network.getId());
        }

        return network;
    }

    protected void setDns(Instance instance, Stack stack, Network network) {
        boolean addDns = DataAccessor.fromMap(DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS))
                .withKey(SystemLabels.LABEL_USE_RANCHER_DNS)
                .withDefault(true)
                .as(Boolean.class);
        if (!addDns) {
            return;
        }

        for (String dns : DataAccessor.fieldStringList(network, NetworkConstants.FIELD_DNS)) {
            List<String> dnsList = appendToFieldStringList(instance, DockerInstanceConstants.FIELD_DNS, dns);
            setField(instance, DockerInstanceConstants.FIELD_DNS, dnsList);
            setField(instance, InstanceConstants.FIELD_DNS_INTERNAL, Joiner.on(",").join(dnsList));
        }

        for (String dnsSearch : fieldStringList(network, NetworkConstants.FIELD_DNS_SEARCH)) {
            List<String> dnsSearchList = appendToFieldStringList(instance, DockerInstanceConstants.FIELD_DNS_SEARCH, dnsSearch);
            setField(instance, DockerInstanceConstants.FIELD_DNS_SEARCH, dnsSearchList);
            String stackDns = null;
            if (stack != null) {
                stackDns = ServiceUtil.getStackNamespace(stack.getName());
            }
            if (!dnsSearchList.contains(stackDns)) {
                dnsSearchList.add(stackDns);
            }
            setField(instance, InstanceConstants.FIELD_DNS_SEARCH_INTERNAL, Joiner.on(",").join(dnsSearchList));
        }
    }

    private void setupCNILabels(Instance instance, Network network) {
        if (!NetworkConstants.KIND_CNI.equals(network.getKind())) {
            String wait = getLabel(instance, SystemLabels.CNI_WAIT);
            String netName = getLabel(instance, SystemLabels.LABEL_CNI_NETWORK);
            if (StringUtils.isBlank(wait) || StringUtils.isBlank(netName)) {
                setLabel(instance, SystemLabels.CNI_WAIT, "true");
                setLabel(instance, SystemLabels.LABEL_CNI_NETWORK, network.getName());
            }
        }
    }

    private void releaseMacAddress(Instance instance, Network network) {
        poolManager.releaseResource(network, instance, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC));
    }

    private void assignMacAddress(Instance instance, Network network) throws LifecycleException {
        String mac = fieldString(instance, InstanceConstants.FIELD_PRIMARY_MAC_ADDRESSS);
        if (StringUtils.isNotBlank(mac)) {
            return;
        }

        PooledResource resource = poolManager.allocateOneResource(network, instance, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC));
        if (resource == null) {
            throw new LifecycleException("Failed to allocate MAC from network");
        }

        setField(instance, InstanceConstants.FIELD_PRIMARY_MAC_ADDRESSS, resource.getName());
        setLabel(instance, SystemLabels.MAC_ADDRESS, resource.getName());
    }

    private void releaseIpAddress(Instance instance, Network network) {
        networkService.releaseIpAddress(network, instance);
    }

    private void assignIpAddress(Instance instance, Network network) throws LifecycleException {
        String ipAddress = fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
        if (StringUtils.isNotBlank(ipAddress) || !networkService.shouldAssignIpAddress(network)) {
            return;
        }

        IPAssignment assignment = allocateIp(instance, network);
        if (assignment != null) {
            setField(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS, assignment.getIpAddress());
            setField(instance, InstanceConstants.FIELD_MANAGED_IP, "true");
            setLabel(instance, SystemLabels.IP_ADDRESS, assignment.getIpAddress());
        }
    }

    private IPAssignment allocateIp(Instance instance, Network network) throws LifecycleException {
        IPAssignment ip = null;
        String requestedIp = null;
        if (instance != null) {
            String allocatedIpAddress = fieldString(instance, InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS);
            if (allocatedIpAddress != null) {
                ip = new IPAssignment(allocatedIpAddress, null);
            }
            requestedIp = fieldString(instance, InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
        }

        if (ip == null) {
            ip = networkService.assignIpAddress(network, instance, requestedIp);
            if (ip == null) {
                throw new LifecycleException("Failed to allocate IP from subnet");
            }
        }

        return ip;
    }

    private void setPskForIpsec(Instance instance) {
        if (!Boolean.TRUE.equals(instance.getSystem())) {
            return;
        }

        List<Long> agentIds = envResourceManager.getAgentProviderIgnoreHealth(SystemLabels.LABEL_AGENT_SERVICE_IPSEC,
                instance.getAccountId());
        for (long agentId : agentIds) {
            ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Agent.class, agentId);
            ConfigUpdateItem item = request.addItem("psk");
            item.setApply(true);
            item.setIncrement(false);
            statusManager.updateConfig(request);
        }
    }

}
