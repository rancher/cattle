package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lb.instance.dao.LoadBalancerInstanceDao;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class LoadBalancerInstanceManagerImpl implements LoadBalancerInstanceManager {

    static final DynamicStringProperty LB_INSTANCE_NAME = ArchaiusUtil.getString("lb.instance.name");
    static final DynamicStringProperty LB_INSTANCE_RESTART_TIMEOUT = ArchaiusUtil
            .getString("lb.instance.restart.default.wait.millis");

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

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer, Long... hostIds) {
        List<Instance> result = new ArrayList<Instance>();
        List<Long> hosts = populateHosts(loadBalancer, hostIds);
        Network network = lbInstanceDao.getLoadBalancerInstanceNetwork(loadBalancer);

        for (long hostId : hosts) {
            Instance lbInstance = getLoadBalancerInstance(loadBalancer, hostId);

            if (lbInstance == null) {
                Host host = objectManager.loadResource(Host.class, hostId);

                String imageUUID = DataAccessor.fields(loadBalancer).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID).as(String.class);

                Map<String, Object> params = new HashMap<>();
                List<Long> networkIds = new ArrayList<>();
                networkIds.add(network.getId());
                params.put(InstanceConstants.FIELD_NETWORK_IDS, networkIds);
                params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);

                // create lb agent (instance will be created along)
                lbInstance = agentInstanceFactory.newBuilder().withAccountId(loadBalancer.getAccountId()).withZoneId(host.getZoneId()).withPrivileged(true)
                        .withUri(getUri(loadBalancer, hostId)).withName(LB_INSTANCE_NAME.get()).withImageUuid(imageUUID).withParameters(params)
                        .withSystemContainerType(SystemContainer.LoadBalancerAgent).build();
            } else {
                start(lbInstance);
            }

            if (lbInstance != null) {
                result.add(lbInstance);
            }
        }

        return result;
    }

    @Override
    public Instance getLoadBalancerInstance(LoadBalancer loadBalancer, long hostId) {
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

    @Override
    public List<Agent> getLoadBalancerAgents(LoadBalancer loadBalancer) {
        List<Agent> agents = new ArrayList<>();
        List<Long> hostIds = lbInstanceDao.getLoadBalancerHosts(loadBalancer.getId());
        for (Long hostId : hostIds) {
            agents.add(getLoadBalancerAgent(loadBalancer, hostId));
        }
        return agents;
    }

    @Override
    public List<Instance> getLoadBalancerInstances(LoadBalancer loadBalancer) {
        List<Instance> instances = new ArrayList<>();
        List<Long> hosts = populateHosts(loadBalancer);
        for (long hostId : hosts) {
            Instance lbInstance = getLoadBalancerInstance(loadBalancer, hostId);
            if (lbInstance != null) {
                instances.add(lbInstance);
            }
        }
        return instances;
    }

    private List<Long> populateHosts(LoadBalancer loadBalancer, Long... hostIds) {
        List<Long> hosts = new ArrayList<Long>();
        if (hostIds.length == 0) {
            hosts = lbInstanceDao.getLoadBalancerHosts(loadBalancer.getId());
        } else {
            hosts.addAll(Arrays.asList(hostIds));
        }
        return hosts;
    }

    protected void start(final Instance agentInstance) {
        if (InstanceConstants.STATE_STOPPED.equals(agentInstance.getState())) {
            DeferredUtils.nest(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    processManager.executeProcess(InstanceConstants.PROCESS_START, agentInstance, null);
                    return null;
                }
            });
        }
    }

    protected String getUri(LoadBalancer lb, long hostId) {
        String uriPredicate = DataAccessor.fields(lb).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE).withDefault("delegate:///").as(
                String.class);
        return String.format("%s?lbId=%d&hostId=%d", uriPredicate, lb.getId(), hostId);
    }

    @Override
    public boolean isLbInstance(Instance instance) {
        if (instance.getAgentId() == null) {
            return false;
        }
        String type = instance.getSystemContainer();
        return (type != null && type.equalsIgnoreCase(SystemContainer.LoadBalancerAgent.name()));
    }

    @Override
    public LoadBalancer getLoadBalancerForInstance(Instance lbInstance) {
        if (!isLbInstance(lbInstance)) {
            return null;
        }
        Agent agent = objectManager.loadResource(Agent.class, lbInstance.getAgentId());

        // get lb id from agent uri
        String uri = agent.getUri();
        String[] result = uri.split("lbId|&");
        Long lbId = Long.valueOf(result[1].substring(result[1].indexOf("=") + 1));
        return objectManager.loadResource(LoadBalancer.class, lbId);
    }

    @Override
    public IpAddress getLoadBalancerInstanceIp(Instance lbInstance) {
        IpAddress ip = null;
        for (Nic nic : objectManager.children(lbInstance, Nic.class)) {
            ip = ipAddressDao.getPrimaryIpAddress(nic);
            if (ip != null) {
                break;
            }
        }
        return ip;
    }

    @Override
    public void restartLoadBalancer(LoadBalancer lb) {
        List<? extends Instance> lbInstances = getLoadBalancerInstances(lb);
        for (final Instance lbInstance : lbInstances) {
            try {
                processManager.scheduleProcessInstance(InstanceConstants.PROCESS_RESTART, lbInstance, null);
            } catch (ProcessCancelException e) {
            }
            resourceMonitor.waitFor(lbInstance, Long.valueOf(LB_INSTANCE_RESTART_TIMEOUT.get()),
                    new ResourcePredicate<Instance>() {
                        @Override
                        public boolean evaluate(Instance obj) {
                            return InstanceConstants.STATE_RUNNING.equals(obj.getState());
                        }
                    });
        }
    }
}