package io.cattle.platform.agent.instance.factory.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceBuilder;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.agent.instance.factory.lock.AgentInstanceAgentCreateLock;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.storage.service.StorageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class AgentInstanceFactoryImpl implements AgentInstanceFactory {

    AccountDao accountDao;
    ObjectManager objectManager;
    AgentInstanceDao factoryDao;
    LockManager lockManager;
    GenericResourceDao resourceDao;
    StorageService storageService;
    AgentLocator agentLocator;

    @Override
    public AgentInstanceBuilder newBuilder() {
        return new AgentInstanceBuilderImpl(this);
    }

    public Long getAgentGroupId(Instance instance) {
        for ( Host host : objectManager.mappedChildren(instance, Host.class) ) {
            RemoteAgent remoteAgent = agentLocator.lookupAgent(host);
            if ( remoteAgent != null ) {
                Agent agent = objectManager.loadResource(Agent.class, remoteAgent.getAgentId());
                return agent.getAgentGroupId();
            }
        }

        return null;
    }

    protected Instance build(AgentInstanceBuilderImpl builder) {
        Agent agent = getAgent(builder);

        return getInstance(agent, builder);
    }

    private Instance getInstance(Agent agent, AgentInstanceBuilderImpl builder) {
        Instance instance = factoryDao.getInstanceByAgent(agent);

        if ( instance != null ) {
            return instance;
        }

        Map<String,Object> properties = getInstanceProperties(agent, builder);

        return createInstance(agent, properties, builder);
    }

    private Map<String, Object> getInstanceProperties(Agent agent, AgentInstanceBuilderImpl builder) {
        Map<Object,Object> properties = new HashMap<Object, Object>();

        properties.put(INSTANCE.ACCOUNT_ID, getAccountId(builder));
        properties.put(INSTANCE.AGENT_ID, agent.getId());
        properties.put(INSTANCE.IMAGE_ID, getImage(agent, builder));
        properties.put(INSTANCE.ZONE_ID, agent.getZoneId());
        properties.put(INSTANCE.KIND, builder.getInstanceKind());

        properties.put(InstanceConstants.FIELD_VNET_IDS, getVnetIds(agent, builder));

        addAdditionalProperties(properties, agent, builder);

        return objectManager.convertToPropertiesFor(Instance.class, properties);
    }

    protected void addAdditionalProperties(Map<Object, Object> properties, Agent agent, AgentInstanceBuilderImpl builder) {
    }

    protected Long getImage(Agent agent, AgentInstanceBuilderImpl builder) {
        Image image;
        try {
            image = storageService.registerRemoteImage(builder.getImageUuid());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get image [" + builder.getImageUuid() + "]");
        }

        return image == null ? null : image.getId();
    }

    protected Object getVnetIds(Agent agent, AgentInstanceBuilderImpl builder) {
        Long vnetId = builder.getVnetId();
        return vnetId == null ? null : new Long[] { builder.getVnetId() };
    }

    protected Agent getAgent(AgentInstanceBuilderImpl builder) {
        String uri = getUri(builder);
        Agent agent = factoryDao.getAgentByUri(uri);

        if ( agent == null ) {
            agent = createAgent(uri, builder);
        }

        return agent;
    }

    protected Instance createInstance(final Agent agent, final Map<String,Object> properties, final AgentInstanceBuilderImpl builder) {
        return lockManager.lock(new AgentInstanceAgentCreateLock(agent.getUri()), new LockCallback<Instance>() {
            @Override
            public Instance doWithLock() {
                Instance instance = factoryDao.getInstanceByAgent(agent);

                if ( instance == null ) {
                    instance = DeferredUtils.nest(new Callable<Instance>() {
                        @Override
                        public Instance call() throws Exception {
                            return factoryDao.createInstanceForProvider(builder.getNetworkServiceProvider(), properties);
                        }
                    });
                }

                return instance;
            }
        });
    }

    protected Agent createAgent(final String uri, final AgentInstanceBuilderImpl builder) {
        return lockManager.lock(new AgentInstanceAgentCreateLock(uri), new LockCallback<Agent>() {
            @Override
            public Agent doWithLock() {
                Agent agent = factoryDao.getAgentByUri(uri);

                if ( agent == null ) {
                    agent = resourceDao.createAndSchedule(Agent.class,
                            AGENT.URI, uri,
                            AGENT.MANAGED_CONFIG, builder.isManagedConfig(),
                            AGENT.AGENT_GROUP_ID, builder.getAgentGroupId(),
                            AGENT.ZONE_ID, builder.getZoneId());
                }

                return agent;
            }
        });
    }

    protected Long getAccountId(AgentInstanceBuilderImpl builder) {
        if ( builder.isAccountOwned() && builder.getAccountId() != null ) {
            return builder.getAccountId();
        }

        return accountDao.getSystemAccount().getId();
    }

    protected String getUri(AgentInstanceBuilderImpl builder) {
        Long networkServiceProviderId = builder.getNetworkServiceProvider() == null ? null :
            builder.getNetworkServiceProvider().getId();

        return String.format("delegate:///?vnetId=%d&networkServiceProviderId=%d", builder.getVnetId(),
                networkServiceProviderId);
    }

    public AgentInstanceDao getFactoryDao() {
        return factoryDao;
    }

    @Inject
    public void setFactoryDao(AgentInstanceDao factoryDao) {
        this.factoryDao = factoryDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    @Inject
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    @Inject
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

}
