package io.cattle.platform.lb.instance.service.impl;

import static io.cattle.platform.core.model.tables.HostTable.HOST;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.lb.instance.dao.LoadBalancerInstanceDao;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class LoadBalancerInstanceManagerImpl implements LoadBalancerInstanceManager {

    static final DynamicStringProperty LB_INSTANCE_NAME = ArchaiusUtil.getString("lb.instance.name");

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    LoadBalancerInstanceDao lbInstanceDao;

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Override
    public List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer, Long... hostIds) {
        List<Instance> result = new ArrayList<Instance>();
        List<Long> hosts = populateHosts(loadBalancer, hostIds);
        Network network = lbInstanceDao.getLoadBalancerInstanceNetwork(loadBalancer);

        for (long hostId : hosts) {
            Instance lbInstance = getLoadBalancerInstance(loadBalancer, hostId);

            if (lbInstance == null) {
                Host host = objectManager.findOne(Host.class, HOST.ID, hostId);

                String imageUUID = DataAccessor
                        .fields(loadBalancer)
                        .withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID)
                        .as(String.class);

                Map<String, Object> params = new HashMap<>();
                List<Long> networkIds = new ArrayList<>();
                networkIds.add(network.getId());
                params.put(InstanceConstants.FIELD_NETWORK_IDS, networkIds);
                params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);

                // create lb agent (instance will be created along)
                lbInstance = agentInstanceFactory
                        .newBuilder()
                        .withAccountId(loadBalancer.getAccountId())
                        .withZoneId(host.getZoneId())
                        .withPrivileged(true)
                        .withUri(getUri(loadBalancer, hostId))
                        .withName(LB_INSTANCE_NAME.get())
                        .withImageUuid(imageUUID)
                        .withParameters(params)
                        .build();
            } else {
                start(lbInstance);
            }

            if (lbInstance != null) {
                result.add(lbInstance);
            }
        }

        return result;
    }

    protected Instance getLoadBalancerInstance(LoadBalancer loadBalancer, long hostId) {
        Agent lbAgent = getLoadBalancerAgent(loadBalancer, hostId);
        Instance lbInstance = null;
        if (lbAgent != null) {
            lbInstance = agentInstanceDao.getInstanceByAgent(lbAgent);
        }
        return lbInstance;
    }

    protected Agent getLoadBalancerAgent(LoadBalancer loadBalancer, long hostId) {
        String uri = getUri(loadBalancer, hostId);
        Agent lbAgent = agentInstanceDao.getAgentByUri(uri);
        return lbAgent;
    }

    private List<Long> populateHosts(LoadBalancer loadBalancer, Long... hostIds) {
        List<Long> hosts = new ArrayList<Long>();
        if (hostIds == null) {
            hosts = lbInstanceDao.getLoadBalancerHosts(loadBalancer.getId());
        } else {
            hosts.addAll(Arrays.asList(hostIds));
        }
        return hosts;
    }

    protected void start(final Instance lbInstance) {
        if (InstanceConstants.STATE_STOPPED.equals(lbInstance.getState())) {
            processManager.executeProcess(InstanceConstants.PROCESS_START, lbInstance, null);
        }
    }

    protected String getUri(LoadBalancer lb, long hostId) {
        String uriPredicate = DataAccessor
                .fields(lb)
                .withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE)
                .withDefault("delegate:///")
                .as(String.class);
        return String.format("%s?lbId=%d&hostId=%d", uriPredicate, lb.getId(),
                hostId);
    }

    @Override
    public boolean isLbInstance(Instance instance) {
        if (instance.getName() == null || instance.getAgentId() == null) {
            return false;
        }
        return instance.getName().equalsIgnoreCase(LB_INSTANCE_NAME.get()) ? true
                : false;
    }
}
