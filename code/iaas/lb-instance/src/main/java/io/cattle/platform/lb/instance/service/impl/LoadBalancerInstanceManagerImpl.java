package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.lb.instance.dao.LoadBalancerInstanceDao;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    LoadBalancerDao lbDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends Instance> createLoadBalancerInstances(LoadBalancer loadBalancer) {
        List<Instance> result = new ArrayList<Instance>();
        Network network = ntwkDao.getNetworkForObject(loadBalancer, NetworkConstants.KIND_HOSTONLY);
        if (network == null) {
            throw new RuntimeException(
                    "Unable to find a network to start a load balancer " + loadBalancer);
        }
        List<? extends LoadBalancerHostMap> hostMaps = lbInstanceDao.getLoadBalancerHostMaps(loadBalancer.getId());
        for (LoadBalancerHostMap hostMap : hostMaps) {
            if (hostMap.getHostId() != null) {
                Host host = objectManager.loadResource(Host.class, hostMap.getHostId());
                if (!host.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                    // skip inactive host
                    continue;
                }
            }

            Instance lbInstance = getLoadBalancerInstance(loadBalancer, hostMap);

            if (lbInstance == null) {

                String imageUUID = DataAccessor.fields(loadBalancer).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID).as(String.class);

                Map<String, Object> params = new HashMap<>();
                // currently we respect only labels parameter from instance launch config when create lb instance
                Map<String, Object> launchConfigData = (Map<String, Object>) DataUtils.getFields(loadBalancer).get(
                        "launchConfig");
                if (launchConfigData != null && launchConfigData.get(InstanceConstants.FIELD_LABELS) != null) {
                    params.put(InstanceConstants.FIELD_LABELS, launchConfigData.get(InstanceConstants.FIELD_LABELS));
                }
                List<Long> networkIds = new ArrayList<>();
                networkIds.add(network.getId());
                params.put(InstanceConstants.FIELD_NETWORK_IDS, networkIds);
                if (hostMap.getHostId() != null) {
                    params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostMap.getHostId());
                }

                // set inital set of lb ports
                List<String> ports = getLbInstancePorts(lbInstance, loadBalancer);
                if (!ports.isEmpty()) {
                    params.put(InstanceConstants.FIELD_PORTS, ports);
                }

                // create lb agent (instance will be created along)
                // following logic from SpecialFieldsPostInstantiationHandler when default zoneId to 1L
                lbInstance = agentInstanceFactory.newBuilder().withAccountId(loadBalancer.getAccountId())
                        .withZoneId(1L).withPrivileged(true)
                        .withUri(getUri(loadBalancer, hostMap)).withName(LB_INSTANCE_NAME.get()).withImageUuid(imageUUID).withParameters(params)
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
    public Instance getLoadBalancerInstance(LoadBalancer loadBalancer, LoadBalancerHostMap hostMap) {
        Agent lbAgent = getLoadBalancerAgent(loadBalancer, hostMap);
        Instance lbInstance = null;
        if (lbAgent != null) {
            lbInstance = agentInstanceDao.getInstanceByAgent(lbAgent);
        }
        return lbInstance;
    }

    protected Agent getLoadBalancerAgent(LoadBalancer loadBalancer, LoadBalancerHostMap hostMap) {
        String uri = getUri(loadBalancer, hostMap);
        Agent lbAgent = agentInstanceDao.getAgentByUri(uri);
        return lbAgent;
    }

    @Override
    public List<Agent> getLoadBalancerAgents(LoadBalancer loadBalancer) {
        List<Agent> agents = new ArrayList<>();
        List<? extends LoadBalancerHostMap> hostMaps = lbInstanceDao.getLoadBalancerHostMaps(loadBalancer.getId());
        for (LoadBalancerHostMap hostMap : hostMaps) {
            Agent agent = getLoadBalancerAgent(loadBalancer, hostMap);
            if (agent != null) {
                agents.add(agent);
            }
        }
        return agents;
    }

    @Override
    public List<Instance> getLoadBalancerInstances(LoadBalancer loadBalancer) {
        List<Instance> instances = new ArrayList<>();
        List<? extends LoadBalancerHostMap> hostMaps = lbInstanceDao.getLoadBalancerHostMaps(loadBalancer.getId());
        for (LoadBalancerHostMap hostMap : hostMaps) {
            Instance lbInstance = getLoadBalancerInstance(loadBalancer, hostMap);
            if (lbInstance != null) {
                instances.add(lbInstance);
            }
        }
        return instances;
    }

    protected void start(final Instance agentInstance) {
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

    protected String getUri(LoadBalancer lb, LoadBalancerHostMap hostMap) {
        String uriPredicate = DataAccessor.fields(lb).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE).withDefault("delegate:///").as(
                String.class);
        return String.format("%s?lbId=%d&hostMapId=%d", uriPredicate, lb.getId(), hostMap.getId());
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

    private List<String> getLbInstancePorts(Instance instance, LoadBalancer lb) {
        List<? extends LoadBalancerListener> listeners = lbDao.listActiveListenersForConfig(lb
                .getLoadBalancerConfigId());
        List<String> ports = new ArrayList<String>();
        for (LoadBalancerListener listener : listeners) {
            String fullPort = listener.getSourcePort() + ":" + listener.getSourcePort();
            ports.add(fullPort);
        }

        return ports;
    }

    @Override
    public LoadBalancerHostMap getLoadBalancerHostMapForInstance(Instance lbInstance) {
        if (!isLbInstance(lbInstance)) {
            return null;
        }
        Agent agent = objectManager.loadResource(Agent.class, lbInstance.getAgentId());

        // get lb id from agent uri
        String uri = agent.getUri();
        String[] result = uri.split("hostMapId=");
        Long hostMapId = Long.valueOf(result[1]);
        return objectManager.loadResource(LoadBalancerHostMap.class, hostMapId);
    }
}