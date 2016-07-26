package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.agent.instance.service.NetworkServiceInfo;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class AgentInstanceManagerImpl implements AgentInstanceManager {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    AgentInstanceFactory agentInstanceFactory;
    AgentInstanceDao agentInstanceDao;
    GenericResourceDao genericResourceDao;
    ResourceMonitor resourceMonitor;
    List<InstanceNicLookup> nicLookups;

    @Override
    public Map<NetworkServiceProvider, Instance> getAgentInstances(Nic nic) {
        return getAgentInstances(nic, false);
    }

    @Override
    public Map<NetworkServiceProvider, Instance> getAgentInstances(Nic nic, boolean includeNetworkAgent) {
        Map<NetworkServiceProvider, Instance> result = new HashMap<NetworkServiceProvider, Instance>();
        Vnet vnet = objectManager.loadResource(Vnet.class, nic.getVnetId());

        if (vnet == null || nic.getNetworkId() == null) {
            return result;
        }

        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());

        if (instance == null) {
            return result;
        }

        Account account = objectManager.loadResource(Account.class, instance.getAccountId());
        List<String> goodStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE);
        if (account == null || !goodStates.contains(account.getState())) {
            return result;
        }

        if (!includeNetworkAgent && instance.getAgentId() != null) {
            if (StringUtils.equalsIgnoreCase(instance.getSystemContainer(),
                    InstanceConstants.SYSTEM_CONTAINER_NETWORK_AGENT)) {
                return result;
            }
        }

        for (NetworkServiceProvider provider : agentInstanceDao.getProviders(nic.getNetworkId())) {
            if (result.containsKey(provider)) {
                continue;
            }

            Instance agentInstance = agentInstanceDao.getAgentInstance(provider, nic);

            if (agentInstance == null) {
                agentInstance = agentInstanceFactory.newBuilder()
                        .withNetworkServiceProvider(provider)
                        .withInstance(instance)
                        .withAccountId(instance.getAccountId())
                        .withPrivileged(true).forVnetId(nic.getVnetId())
                        .withSystemContainerType(InstanceConstants.SYSTEM_CONTAINER_NETWORK_AGENT)
                        .withParameters(CollectionUtils.asMap(InstanceConstants.FIELD_DATA_VOLUMES,
                                Arrays.asList(AgentConstants.AGENT_INSTANCE_BIND_MOUNT)))
                        .build();
            } else {
                start(agentInstance);
            }

            if (agentInstance != null) {
                result.put(provider, agentInstance);
            }
        }

        return result;
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

    @Override
    public NetworkServiceInfo getNetworkService(Instance instance, String kind, boolean waitForStart) {
        if (instance == null || kind == null) {
            return null;
        }

        NetworkServiceInfo info = agentInstanceDao.getNetworkServiceInfo(instance.getId(), kind);

        if (info == null) {
            return null;
        }

        Map<NetworkServiceProvider, Instance> instances = getAgentInstances(info.getClientNic());

        for (Map.Entry<NetworkServiceProvider, Instance> entry : instances.entrySet()) {
            if (entry.getKey().getId().equals(info.getNetworkServiceProvider().getId())) {
                info.setAgentInstance(entry.getValue());
                break;
            }
        }

        if (info.getAgentInstance() == null) {
            return info;
        }

        if (waitForStart) {
            start(info.getAgentInstance());
            instance = resourceMonitor.waitFor(info.getAgentInstance(), new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return InstanceConstants.STATE_RUNNING.equals(obj.getState());
                }

                @Override
                public String getMessage() {
                    return "running";
                }
            });

            info.setAgentInstance(instance);
        }

        agentInstanceDao.populateNicAndIp(info);

        return info;
    }

    @Override
    public List<? extends Agent> getAgents(NetworkServiceProvider provider) {
        return agentInstanceDao.getAgents(provider);
    }

    @Override
    public List<? extends Nic> getNicsFromResource(Object resource) {
        if (resource instanceof Nic) {
            return Arrays.asList((Nic) resource);
        }

        List<? extends Nic> nics = null;

        for (InstanceNicLookup lookup : nicLookups) {
            nics = lookup.getNics(resource);
            if (nics != null) {
                break;
            }
        }

        return nics;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AgentInstanceFactory getAgentInstanceFactory() {
        return agentInstanceFactory;
    }

    @Inject
    public void setAgentInstanceFactory(AgentInstanceFactory agentInstanceFactory) {
        this.agentInstanceFactory = agentInstanceFactory;
    }

    public AgentInstanceDao getAgentInstanceDao() {
        return agentInstanceDao;
    }

    @Inject
    public void setAgentInstanceDao(AgentInstanceDao agentInstanceDao) {
        this.agentInstanceDao = agentInstanceDao;
    }

    public GenericResourceDao getGenericResourceDao() {
        return genericResourceDao;
    }

    @Inject
    public void setGenericResourceDao(GenericResourceDao genericResourceDao) {
        this.genericResourceDao = genericResourceDao;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    @Inject
    public void setResourceMonitor(ResourceMonitor resourceMonitor) {
        this.resourceMonitor = resourceMonitor;
    }

    public List<InstanceNicLookup> getNicLookups() {
        return nicLookups;
    }

    public void setNicLookups(List<InstanceNicLookup> nicLookups) {
        this.nicLookups = nicLookups;
    }

}
