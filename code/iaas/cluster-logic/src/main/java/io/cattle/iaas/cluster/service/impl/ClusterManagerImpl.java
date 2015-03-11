package io.cattle.iaas.cluster.service.impl;

import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class ClusterManagerImpl implements ClusterManager {

    static final DynamicStringProperty CLUSTER_INSTANCE_NAME = ArchaiusUtil.getString("cluster.instance.name");
    static final DynamicStringProperty CLUSTER_IMAGE_NAME = ArchaiusUtil.getString("cluster.image.name");
    static final DynamicStringListProperty CONFIG_ITEMS = ArchaiusUtil.getList("cluster.config.items");

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ConfigItemStatusManager statusManager;

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    AccountDao accountDao;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ObjectManager objectManager;

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Inject
    LockManager lockManager;

    @Override
    public Instance getClusterServerInstance(Host cluster) {
        Agent clusterServerAgent = getClusterServerAgent(cluster);
        Instance clusterServerInstance = null;
        if (clusterServerAgent != null) {
            clusterServerInstance = agentInstanceDao.getInstanceByAgent(clusterServerAgent);
        }
        return clusterServerInstance;
    }

    @Override
    public Agent getClusterServerAgent(Host cluster) {
        String uri = getUri(cluster);
        Agent clusterServerAgent = agentInstanceDao.getAgentByUri(uri);
        return clusterServerAgent;
    }

    @Override
    public IpAddress getClusterServerInstanceIp(Instance clusterServerInstance) {
        IpAddress ip = null;
        for (Nic nic : objectManager.children(clusterServerInstance, Nic.class)) {
            ip = ipAddressDao.getPrimaryIpAddress(nic);
            if (ip != null) {
                break;
            }
        }
        return ip;
    }

    private Instance createClusterServerInstance(Host cluster) {
        Instance clusterServerInstance = getClusterServerInstance(cluster);
        if (clusterServerInstance == null) {
            Host managingHost = getManagingHost(cluster);
            Integer clusterServerPort = DataAccessor.fields(cluster).withKey(ClusterConstants.CLUSTER_SERVER_PORT).as(Integer.class);

            Map<String, Object> params = new HashMap<>();
            params.put(InstanceConstants.FIELD_NETWORK_IDS, Lists.newArrayList(getNetworkIds(managingHost)));
            params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, managingHost.getId());
            params.put(InstanceConstants.FIELD_PORTS, Lists.newArrayList(clusterServerPort + ":" + clusterServerPort + "/tcp"));

            clusterServerInstance = agentInstanceFactory
                    .newBuilder()
                    .withAccountId(managingHost.getAccountId())
                    .withZoneId(managingHost.getZoneId())
                    .withPrivileged(true)
                    .withUri(getUri(cluster, managingHost))
                    .withName(CLUSTER_INSTANCE_NAME.get())
                    .withImageUuid(getImageUuid(cluster, managingHost))
                    .withParameters(params)
                    .withSystemContainerType(SystemContainer.ClusterAgent)
                    .build();
        } else {
            start(clusterServerInstance);
        }

        return clusterServerInstance;
    }

    private String getImageUuid(Host cluster, Host managingHost) {
        return getImagePrefix(cluster, managingHost) + CLUSTER_IMAGE_NAME.get();
    }

    private String getUri(Host cluster, Host managingHost) {
        return String.format("%s?clusterId=%d&managingHostId=%d",
                getConnectionPrefix(cluster, managingHost) + "///", cluster.getId(), managingHost.getId());
    }

    private String getUri(Host cluster) {
        return getUri(cluster, getManagingHost(cluster));
    }

    private String getConnectionPrefix(Host cluster, Host managingHost) {
        return objectManager.isKind(managingHost, "sim") ? "sim:" : "delegate:";
    }

    private String getImagePrefix(Host cluster, Host managingHost) {
        return objectManager.isKind(managingHost, "sim") ? "sim:" : "docker:";
    }

    private Long getNetworkIds(Host managingHost) {
        List<? extends Network> accountNetworks = ntwkDao.getNetworksForAccount(managingHost.getAccountId(),
                NetworkConstants.KIND_HOSTONLY);
        if (accountNetworks.isEmpty()) {
            // pass system network if account doesn't own any
            List<? extends Network> systemNetworks = ntwkDao.getNetworksForAccount(accountDao.getSystemAccount()
                    .getId(),
                    NetworkConstants.KIND_HOSTONLY);
            if (systemNetworks.isEmpty()) {
                throw new RuntimeException(
                        "Unable to find a network to start cluster server");
            }
            return systemNetworks.get(0).getId();
        }

        return accountNetworks.get(0).getId();
    }

    private void start(final Instance agentInstance) {
        if (InstanceConstants.STATE_STOPPED.equals(agentInstance.getState())) {
            DeferredUtils.nest(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    processManager.scheduleProcessInstance(InstanceConstants.PROCESS_START, agentInstance, null);
                    return null;
                }
            });
        }
    }

    @Override
    public Host getManagingHost(Host cluster) {
        Long managingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
        if (managingHostId == null) {
            throw new RuntimeException("Missing managingHostId for cluster:" + cluster.getId());
        }
        return objectManager.loadResource(Host.class, managingHostId);
    }

    @Override
    public void updateClusterServerConfig(ProcessState state, Host cluster) {
        if (!CommonStatesConstants.ACTIVE.equals(cluster.getState()) &&
                !CommonStatesConstants.ACTIVATING.equals(cluster.getState())) {
            return;
        }
        // short term optimization to avoid updating cluster object unnecessarily
        // since we're just currently only supporting file:// discoverySpec
        if (StringUtils.isEmpty(DataAccessor.fieldString(cluster, ClusterConstants.DISCOVERY_SPEC))) {
            DataUtils.getWritableFields(cluster).put(ClusterConstants.DISCOVERY_SPEC, "file:///etc/cluster/cluster-hosts.conf");
            objectManager.persist(cluster);
        }

        Instance clusterServerInstance = createClusterServerInstance(cluster);
        clusterServerInstance = resourceMonitor.waitFor(clusterServerInstance, new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });

        Agent clusterAgent = getClusterServerAgent(cluster);
        if (clusterAgent == null) {
            return;
        }
        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state,
                getContext(clusterAgent));
        request = before(request, clusterAgent);
        ConfigUpdateRequestUtils.setRequest(request, state, getContext(clusterAgent));
        after(request);
    }

    public void activateCluster(ProcessState state, Host cluster) {
        Long hostId = findSuitableHost(cluster);

        DataUtils.getWritableFields(cluster).put(ClusterConstants.MANAGING_HOST, hostId);
        objectManager.persist(cluster);

        if (hostId == null) {
            processManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, cluster, null);
        } else {
            updateClusterServerConfig(state, cluster);
        }

    }

    private Long findSuitableHost(Host cluster) {
        List<ClusterHostMapRecord> mappings = clusterHostMapDao.findClusterHostMapsForCluster(cluster);
        if (mappings.size() == 0) {
            return null;
        }

        for (ClusterHostMapRecord mapping: mappings) {
            Host host = objectManager.loadResource(Host.class, mapping.getHostId());
            if (host != null && CommonStatesConstants.ACTIVE.equals(host.getState())) {
                return mapping.getHostId();
            }
        }
        return null;
    }

    private ConfigUpdateRequest before(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = new ConfigUpdateRequest(agent.getId());
            for (String item : CONFIG_ITEMS.get()) {
                request.addItem(item)
                        .withApply(true)
                        .withIncrement(true)
                        .setCheckInSyncOnly(true);
            }
        }

        statusManager.updateConfig(request);

        return request;
    }

    private void after(ConfigUpdateRequest request) {
        if (request == null) {
            return;
        }

        statusManager.waitFor(request);
    }

    private String getContext(Agent agent) {
        return String.format("AgentUpdateConfig:%s", agent.getId());
    }

}
